package com.islehub.order.service;

import com.islehub.common.exception.BizException;
import com.islehub.common.mq.OrderCreatedEvent;
import com.islehub.order.entity.Order;
import com.islehub.order.entity.OrderItem;
import com.islehub.order.mapper.OrderItemMapper;
import com.islehub.order.mapper.OrderMapper;
import com.islehub.product.entity.ProductSku;
import com.islehub.product.mapper.ProductSkuMapper;
import com.islehub.product.service.StockCacheService;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * C 端下单编排 —— 接收已解析的购物车数据和地址信息，执行下单事务。
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final ProductSkuMapper skuMapper;
    private final StockCacheService stockCacheService;
    private final RabbitTemplate rabbitTemplate;

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
            ProductSku sku = skuMapper.selectById(ci.getSkuId());
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
            item.setPrice(ci.getPrice());
            item.setQuantity(ci.getQuantity());
            orderItems.add(item);
            total = total.add(ci.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        // ③ DB 创建订单（失败则回滚 Redis 库存）
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
        } catch (Exception e) {
            stockCacheService.rollback(skuIds, quantities);
            throw e;
        }

        // ④ 发送 MQ 消息（异步落库确认）
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(order.getId());
        event.setItems(orderItems.stream()
                .map(i -> new OrderCreatedEvent.OrderItemStock(i.getSkuId(), i.getQuantity()))
                .toList());
        rabbitTemplate.convertAndSend("islehub.order.topic", "order.stock.confirm", event);

        return new CheckoutResult(order, warnings);
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
