package com.islehub.shop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.islehub.shop.entity.Address;
import com.islehub.shop.mapper.AddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShopOrderService extends ServiceImpl<OrderMapper, Order> {

    private final OrderItemMapper itemMapper;
    private final OrderShippingMapper shippingMapper;
    private final CartService cartService;
    private final ProductSkuMapper skuMapper;
    private final AddressMapper addressMapper;

    public ShopOrderService(OrderMapper orderMapper, OrderItemMapper itemMapper,
                            OrderShippingMapper shippingMapper, CartService cartService,
                            ProductSkuMapper skuMapper, AddressMapper addressMapper) {
        this.itemMapper = itemMapper;
        this.shippingMapper = shippingMapper;
        this.cartService = cartService;
        this.skuMapper = skuMapper;
        this.addressMapper = addressMapper;
    }

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

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (CartService.CartItem ci : cartItems) {
            ProductSku sku = skuMapper.selectById(ci.getSkuId());
            if (sku == null) {
                throw new BizException("商品 " + ci.getProductName() + " 不存在");
            }
            if (sku.getPrice().compareTo(ci.getPrice()) != 0) {
                warnings.add(ci.getProductName() + " 价格已变动：加购时 ¥" + ci.getPrice()
                        + "，现价 ¥" + sku.getPrice());
            }
            boolean deducted = skuMapper.update(null,
                    new LambdaUpdateWrapper<ProductSku>()
                            .setSql("stock = stock - " + ci.getQuantity())
                            .eq(ProductSku::getId, ci.getSkuId())
                            .ge(ProductSku::getStock, ci.getQuantity())) > 0;
            if (!deducted) {
                throw new BizException("商品 " + ci.getProductName() + " 库存不足");
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
        order.setTotalAmount(total);
        save(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            itemMapper.insert(item);
        }

        cartService.clear(userId);
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
            skuMapper.update(null,
                    new LambdaUpdateWrapper<ProductSku>()
                            .setSql("stock = stock + " + item.getQuantity())
                            .eq(ProductSku::getId, item.getSkuId()));
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

    public static class CheckoutResult {
        private final Order order;
        private final List<String> warnings;

        public CheckoutResult(Order order, List<String> warnings) {
            this.order = order;
            this.warnings = warnings;
        }

        public Order getOrder() { return order; }
        public List<String> getWarnings() { return warnings; }
    }
}
