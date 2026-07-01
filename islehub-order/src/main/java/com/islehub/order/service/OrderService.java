package com.islehub.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.islehub.order.entity.Order;
import com.islehub.order.entity.OrderItem;
import com.islehub.order.entity.OrderShipping;
import java.util.List;

public interface OrderService extends IService<Order> {
    // ── 管理端 ──
    Page<Order> pageOrders(int page, int pageSize, String orderNo, String status, String startDate, String endDate);
    Order getDetail(Long id);
    void createOrder(Order order, List<OrderItem> items);
    void updateStatus(Long id, String status);
    void cancelOrder(Long id);
    void addShipping(Long orderId, String carrier, String trackingNo);
    List<Order> exportOrders(String orderNo, String status, String startDate, String endDate);
    byte[] exportOrdersExcel(String orderNo, String status, String startDate, String endDate);

    // ── C 端 ──
    Page<Order> pageUserOrders(int page, int pageSize, Long userId, String status);
    Order getUserOrderDetail(Long id, Long userId);
    void cancelUserOrder(Long id, Long userId);
    void confirmUserOrder(Long id, Long userId);
}
