package com.islehub.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.islehub.common.exception.BizException;
import com.islehub.common.redis.CacheService;
import com.islehub.common.redis.RedisKeys;
import com.islehub.order.entity.Order;
import com.islehub.order.entity.OrderItem;
import com.islehub.order.entity.OrderShipping;
import com.islehub.order.mapper.OrderItemMapper;
import com.islehub.order.mapper.OrderMapper;
import com.islehub.order.mapper.OrderShippingMapper;
import com.islehub.order.service.OrderService;
import com.islehub.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "pending",   Set.of("paid", "cancelled"),
            "paid",      Set.of("shipped", "cancelled"),
            "shipped",   Set.of("delivered", "completed"),
            "delivered", Set.of("completed")
    );

    private final OrderItemMapper itemMapper;
    private final OrderShippingMapper shippingMapper;
    private final ProductService productService;
    private final CacheService cacheService;

    @Override
    public Page<Order> pageOrders(int page, int pageSize, String orderNo, String status,
                                   String startDate, String endDate) {
        return baseMapper.pageOrders(new Page<>(page, pageSize),
                orderNo, status,
                StringUtils.hasText(startDate) ? startDate + " 00:00:00" : null,
                StringUtils.hasText(endDate) ? endDate + " 23:59:59" : null);
    }

    @Override
    public Order getDetail(Long id) {
        String key = RedisKeys.orderDetail(id);
        Order order = cacheService.getOrLoad(key, Order.class, RedisKeys.ORDER_DETAIL_TTL, () -> {
            return baseMapper.selectDetailById(id);
        });
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    @Override
    @Transactional
    public void createOrder(Order order, List<OrderItem> items) {
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setStatus("pending");
        BigDecimal total = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        save(order);
        items.forEach(i -> i.setOrderId(order.getId()));
        items.forEach(itemMapper::insert);
        // 新订单无需失效缓存
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        Order order = getById(id);
        if (order == null) throw new BizException("订单不存在");

        Set<String> allowed = ALLOWED_TRANSITIONS.get(order.getStatus());
        if (allowed == null || !allowed.contains(status)) {
            throw new BizException("不允许从 " + order.getStatus() + " 变更为 " + status);
        }

        order.setStatus(status);
        updateById(order);
        if ("delivered".equals(status) || "completed".equals(status)) {
            OrderShipping shipping = shippingMapper.selectOne(
                    new LambdaQueryWrapper<OrderShipping>().eq(OrderShipping::getOrderId, id));
            if (shipping != null) {
                shipping.setDeliveredAt(LocalDateTime.now());
                shippingMapper.updateById(shipping);
            }
        }

        // 状态变更后失效缓存
        cacheService.evict(RedisKeys.orderDetail(id));
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        updateStatus(id, "cancelled");
        // updateStatus 内部已失效缓存
    }

    @Override
    @Transactional
    public void addShipping(OrderShipping shipping) {
        shipping.setShippedAt(LocalDateTime.now());
        shippingMapper.insert(shipping);
        updateStatus(shipping.getOrderId(), "shipped");
        // updateStatus 内部已失效缓存
    }

    @Override
    public List<Order> exportOrders(String orderNo, String status, String startDate, String endDate) {
        return baseMapper.pageOrders(new Page<>(1, Integer.MAX_VALUE),
                orderNo, status,
                StringUtils.hasText(startDate) ? startDate + " 00:00:00" : null,
                StringUtils.hasText(endDate) ? endDate + " 23:59:59" : null)
                .getRecords();
    }

    @Override
    public byte[] exportOrdersExcel(String orderNo, String status, String startDate, String endDate) {
        List<Order> orders = exportOrders(orderNo, status, startDate, endDate);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("订单数据");
            Row header = sheet.createRow(0);
            String[] columns = {"订单号", "用户ID", "金额", "状态", "收货人", "电话", "地址", "备注", "创建时间"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            for (int i = 0; i < orders.size(); i++) {
                Order o = orders.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(o.getOrderNo() != null ? o.getOrderNo() : "");
                row.createCell(1).setCellValue(o.getUserId() != null ? o.getUserId().toString() : "");
                row.createCell(2).setCellValue(o.getTotalAmount() != null ? o.getTotalAmount().toString() : "");
                row.createCell(3).setCellValue(o.getStatus() != null ? o.getStatus() : "");
                row.createCell(4).setCellValue(o.getReceiverName() != null ? o.getReceiverName() : "");
                row.createCell(5).setCellValue(o.getReceiverPhone() != null ? o.getReceiverPhone() : "");
                row.createCell(6).setCellValue(o.getReceiverAddress() != null ? o.getReceiverAddress() : "");
                row.createCell(7).setCellValue(o.getRemark() != null ? o.getRemark() : "");
                row.createCell(8).setCellValue(o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("导出Excel失败");
        }
    }

    // ──────────────── C 端方法 ────────────────

    @Override
    public Page<Order> pageUserOrders(int page, int pageSize, Long userId, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Order getUserOrderDetail(Long id, Long userId) {
        // 复用 getDetail（已走缓存），再做用户权限校验
        Order order = getDetail(id);
        if (!order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    @Override
    @Transactional
    public void cancelUserOrder(Long id, Long userId) {
        Order order = getById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        if (!"paid".equals(order.getStatus())) {
            throw new BizException("只能取消已支付未发货的订单");
        }
        order.setStatus("cancelled");
        updateById(order);
        cacheService.evict(RedisKeys.orderDetail(id));

        // 恢复 DB 库存
        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        for (OrderItem item : items) {
            productService.addSkuStock(item.getSkuId(), item.getQuantity());
        }
    }

    @Override
    @Transactional
    public void confirmUserOrder(Long id, Long userId) {
        Order order = getById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        if (!"shipped".equals(order.getStatus())) {
            throw new BizException("只能确认已发货的订单");
        }
        order.setStatus("completed");
        updateById(order);

        // 失效缓存
        cacheService.evict(RedisKeys.orderDetail(id));

        OrderShipping shipping = shippingMapper.selectOne(
                new LambdaQueryWrapper<OrderShipping>().eq(OrderShipping::getOrderId, id));
        if (shipping != null) {
            shipping.setDeliveredAt(LocalDateTime.now());
            shippingMapper.updateById(shipping);
        }
    }
}
