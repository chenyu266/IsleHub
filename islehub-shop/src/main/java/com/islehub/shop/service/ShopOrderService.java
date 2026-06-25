package com.islehub.shop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.islehub.common.exception.BizException;
import com.islehub.order.entity.Order;
import com.islehub.order.entity.OrderItem;
import com.islehub.order.entity.OrderShipping;
import com.islehub.order.mapper.OrderItemMapper;
import com.islehub.order.mapper.OrderMapper;
import com.islehub.order.mapper.OrderShippingMapper;
import com.islehub.product.entity.ProductSku;
import com.islehub.product.mapper.ProductSkuMapper;
import com.islehub.common.mq.OrderCreatedEvent;
import com.islehub.shop.entity.Address;
import com.islehub.shop.mapper.AddressMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopOrderService extends ServiceImpl<OrderMapper, Order> {

    private final OrderItemMapper itemMapper;
    private final OrderShippingMapper shippingMapper;
    private final CartService cartService;
    private final ProductSkuMapper skuMapper;
    private final AddressMapper addressMapper;
    private final StockCacheService stockCacheService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public CheckoutResult checkout(Long userId, Long addressId, String remark) {
        List<CartService.CartItem> cartItems = cartService.list(userId);
        if (cartItems.isEmpty()) {
            throw new BizException("购物车为空");
        }
        Address address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException("收货地址不存在");
        }

        // ① Redis 原子预扣库存（成功才继续）
        List<Long> skuIds = cartItems.stream().map(CartService.CartItem::getSkuId).toList();
        List<Integer> quantities = cartItems.stream().map(CartService.CartItem::getQuantity).toList();
        stockCacheService.tryDeduct(skuIds, quantities);

        // ② 构建订单数据
        List<String> warnings = new ArrayList<>();
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartService.CartItem ci : cartItems) {
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
            items.add(item);
            total = total.add(ci.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        // ③ DB 创建订单（失败则回滚 Redis 库存）
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setUserId(userId);
        order.setStatus("paid");
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(
                (address.getProvince() != null ? address.getProvince() : "") +
                (address.getCity() != null ? address.getCity() : "") +
                (address.getDistrict() != null ? address.getDistrict() : "") +
                address.getDetail());
        order.setRemark(remark);
        order.setTotalAmount(total);

        try {
            save(order);
            for (OrderItem item : items) {
                item.setOrderId(order.getId());
                itemMapper.insert(item);
            }
        } catch (Exception e) {
            stockCacheService.rollback(skuIds, quantities);
            throw e;
        }

        // ④ 清空购物车
        cartService.clear(userId);

        // ⑤ 发送 MQ 消息（异步落库确认）
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(order.getId());
        event.setItems(items.stream()
                .map(i -> new OrderCreatedEvent.OrderItemStock(i.getSkuId(), i.getQuantity()))
                .toList());
        rabbitTemplate.convertAndSend("islehub.order.topic", "order.stock.confirm", event);

        return new CheckoutResult(order, warnings);
    }

    public Page<Order> pageOrders(int page, int pageSize, Long userId, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return page(new Page<>(page, pageSize), wrapper);
    }

    public Order getDetail(Long id, Long userId) {
        Order order = baseMapper.selectDetailById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    @Transactional
    public void cancelOrder(Long id, Long userId) {
        Order order = getById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        if (!"paid".equals(order.getStatus())) {
            throw new BizException("只能取消已支付未发货的订单");
        }
        order.setStatus("cancelled");
        updateById(order);

        // 恢复库存
        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        for (OrderItem item : items) {
            skuMapper.addStock(item.getSkuId(), item.getQuantity());
        }
    }

    public void confirmOrder(Long id, Long userId) {
        Order order = getById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        if (!"shipped".equals(order.getStatus())) {
            throw new BizException("只能确认已发货的订单");
        }
        order.setStatus("completed");
        updateById(order);
        OrderShipping shipping = shippingMapper.selectOne(
                new LambdaQueryWrapper<OrderShipping>().eq(OrderShipping::getOrderId, id));
        if (shipping != null) {
            shipping.setDeliveredAt(LocalDateTime.now());
            shippingMapper.updateById(shipping);
        }
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
