package com.islehub.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islehub.common.exception.BizException;
import com.islehub.common.mq.OrderCreatedEvent;
import com.islehub.order.entity.Order;
import com.islehub.order.entity.OrderItem;
import com.islehub.order.entity.OrderMessage;
import com.islehub.order.mapper.OrderItemMapper;
import com.islehub.order.mapper.OrderMapper;
import com.islehub.order.mapper.OrderMessageMapper;
import com.islehub.product.entity.ProductSku;
import com.islehub.product.service.ProductService;
import com.islehub.product.service.StockCacheService;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 结算服务，负责下单过程中的库存预扣、订单创建及MQ消息发送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final OrderMessageMapper orderMessageMapper;
    private final ProductService productService;
    private final StockCacheService stockCacheService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public CheckoutResult checkout(Long userId, String receiverName, String receiverPhone,
                                   String receiverAddress, String remark,
                                   List<CheckoutItem> items) {
        if (items.isEmpty()) {
            throw new BizException("订单商品不能为空");
        }

        // ① Redis 原子预扣库存
        List<Long> skuIds = items.stream().map(CheckoutItem::getSkuId).toList();
        List<Integer> quantities = items.stream().map(CheckoutItem::getQuantity).toList();
        stockCacheService.tryDeduct(skuIds, quantities);

        // ② 校验 SKU 并计算总价
        List<String> warnings = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CheckoutItem ci : items) {
            ProductSku sku = productService.getSkuById(ci.getSkuId());
            if (sku == null) {
                throw new BizException("商品 " + ci.getProductName() + " 不存在");
            }
            if (sku.getPrice().compareTo(ci.getPrice()) != 0) {
                warnings.add(ci.getProductName() + " 价格已变动：加购时 ¥" + ci.getPrice()
                        + "，现价 ¥" + sku.getPrice());
            }
            OrderItem item = new OrderItem();
            item.setProductId(ci.getProductId());
            item.setSkuId(ci.getSkuId());
            item.setProductName(ci.getProductName());
            item.setSkuSpec(ci.getSkuSpec());
            item.setPrice(sku.getPrice());
            item.setQuantity(ci.getQuantity());
            orderItems.add(item);
            total = total.add(sku.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        // ③ DB 创建订单
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setUserId(userId);
        order.setStatus("paid");
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setRemark(remark);
        order.setTotalAmount(total);

        try {
            orderMapper.insert(order);
            for (OrderItem item : orderItems) {
                item.setOrderId(order.getId());
                itemMapper.insert(item);
            }

            // ④ 在事务内写入本地消息表
            OrderCreatedEvent event = new OrderCreatedEvent();
            event.setOrderId(order.getId());
            event.setItems(orderItems.stream()
                    .map(i -> new OrderCreatedEvent.OrderItemStock(i.getSkuId(), i.getQuantity()))
                    .toList());

            OrderMessage orderMessage = new OrderMessage();
            orderMessage.setOrderId(order.getId());
            orderMessage.setMessageBody(objectMapper.writeValueAsString(event));
            orderMessage.setStatus(0);
            orderMessage.setRetryCount(0);
            orderMessage.setCreatedAt(LocalDateTime.now());
            orderMessage.setUpdatedAt(LocalDateTime.now());
            orderMessageMapper.insert(orderMessage);

        } catch (Exception e) {
            stockCacheService.rollback(skuIds, quantities);
            throw new BizException("订单创建失败：" + e.getMessage(), e);
        }

        // ⑤ 事务提交后再发 MQ
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendOrderMessage(order.getId());
            }
        });

        return new CheckoutResult(order, warnings);
    }
    /**
     * 发送订单消息（供事务提交后回调和定时任务调用）
     */
    public void sendOrderMessage(Long orderId) {
        // 原子认领消息（UPDATE status=1 WHERE status=0），防止 afterCommit 与定时任务并发
        int claimed = orderMessageMapper.claimForSending(orderId);
        if (claimed == 0) {
            log.warn("订单消息已被发送或不存在, orderId={}", orderId);
            return;
        }

        OrderMessage message = orderMessageMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderMessage>()
                        .eq(OrderMessage::getOrderId, orderId));
        if (message == null) {
            return;
        }

        try {
            OrderCreatedEvent event = objectMapper.readValue(message.getMessageBody(), OrderCreatedEvent.class);
            rabbitTemplate.convertAndSend("islehub.order.topic", "order.stock.confirm", event);
            log.info("订单消息发送成功, orderId={}", orderId);
        } catch (Exception e) {
            log.error("订单消息发送失败, orderId={}", orderId, e);

            // 发送失败，回退状态供重试
            message.setStatus(0);
            message.setRetryCount(message.getRetryCount() + 1);
            message.setErrorMsg(e.getMessage());
            if (message.getRetryCount() >= 3) {
                message.setStatus(2);
            }
            message.setUpdatedAt(LocalDateTime.now());
            orderMessageMapper.updateById(message);
        }
    }

    // ──────────────── DTO / Result ────────────────

    @Data
    public static class CheckoutItem {
        private Long skuId;
        private Long productId;
        private String productName;
        private String skuSpec;
        private BigDecimal price;
        private Integer quantity;
    }

    @Getter
    public static class CheckoutResult {
        private final Order order;
        private final List<String> warnings;

        public CheckoutResult(Order order, List<String> warnings) {
            this.order = order;
            this.warnings = warnings;
        }
    }
}

