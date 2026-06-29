package com.islehub.shop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.common.exception.BizException;
import com.islehub.order.entity.Order;
import com.islehub.order.service.CheckoutService;
import com.islehub.order.service.CheckoutService.CheckoutItem;
import com.islehub.order.service.CheckoutService.CheckoutResult;
import com.islehub.order.service.OrderService;
import com.islehub.shop.entity.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * C 端订单编排 —— 读购物车和地址 → 委托 order 模块的 CheckoutService，其余委托 OrderService。
 */
@Service
@RequiredArgsConstructor
public class ShopOrderService {

    private final CheckoutService checkoutService;
    private final OrderService orderService;
    private final CartService cartService;
    private final AddressService addressService;

    @Transactional
    public CheckoutResult checkout(Long userId, Long addressId, String remark) {
        // ① 读购物车
        List<CartService.CartItem> cartItems = cartService.list(userId);
        if (cartItems.isEmpty()) {
            throw new BizException("购物车为空");
        }

        // ② 读地址
        Address address = addressService.getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException("收货地址不存在");
        }

        // ③ 映射 CartItem → CheckoutItem
        List<CheckoutItem> items = cartItems.stream().map(ci -> {
            CheckoutItem item = new CheckoutItem();
            item.setSkuId(ci.getSkuId());
            item.setProductId(ci.getProductId());
            item.setProductName(ci.getProductName());
            item.setSkuSpec(ci.getSkuSpec());
            item.setPrice(ci.getPrice());
            item.setQuantity(ci.getQuantity());
            return item;
        }).toList();

        // ④ 调用 order 模块下单
        String receiverAddress = (address.getProvince() != null ? address.getProvince() : "")
                + (address.getCity() != null ? address.getCity() : "")
                + (address.getDistrict() != null ? address.getDistrict() : "")
                + address.getDetail();
        CheckoutResult result = checkoutService.checkout(
                userId, address.getReceiverName(), address.getReceiverPhone(),
                receiverAddress, remark, items);

        // ⑤ 清空购物车
        cartService.clear(userId);

        return result;
    }

    public Page<Order> pageOrders(int page, int pageSize, Long userId, String status) {
        return orderService.pageUserOrders(page, pageSize, userId, status);
    }

    public Order getDetail(Long id, Long userId) {
        return orderService.getUserOrderDetail(id, userId);
    }

    public void cancelOrder(Long id, Long userId) {
        orderService.cancelUserOrder(id, userId);
    }

    public void confirmOrder(Long id, Long userId) {
        orderService.confirmUserOrder(id, userId);
    }
}
