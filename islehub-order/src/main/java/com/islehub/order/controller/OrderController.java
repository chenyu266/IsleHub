package com.islehub.order.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.common.result.R;
import com.islehub.order.entity.Order;
import com.islehub.order.entity.OrderItem;
import com.islehub.order.entity.OrderShipping;
import com.islehub.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@SaCheckRole(value = {"admin", "operator"}, mode = SaMode.OR)
@Tag(name = "管理-订单", description = "订单CRUD、状态流转、导出")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "分页查询订单")
    @GetMapping("/page")
    public R<R.PageResult<Order>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<Order> result = orderService.pageOrders(page, pageSize, orderNo, status, startDate, endDate);
        return R.page(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<Order> detail(@Parameter(description = "订单ID") @PathVariable Long id) {
        return R.ok(orderService.getDetail(id));
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public R<Void> create(@Valid @RequestBody OrderCreateDTO dto) {
        orderService.createOrder(dto.getOrder(), dto.getItems());
        return R.ok();
    }

    @Operation(summary = "更新订单状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@Parameter(description = "订单ID") @PathVariable Long id,
                                @Valid @RequestBody StatusDTO dto) {
        orderService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@Parameter(description = "订单ID") @PathVariable Long id) {
        orderService.cancelOrder(id);
        return R.ok();
    }

    @Operation(summary = "订单发货")
    @PostMapping("/{id}/shipping")
    public R<Void> shipping(@Parameter(description = "订单ID") @PathVariable Long id,
                            @Valid @RequestBody OrderShipping shipping) {
        shipping.setOrderId(id);
        orderService.addShipping(shipping);
        return R.ok();
    }

    @SaCheckRole("admin")
    @Operation(summary = "导出订单 Excel")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        byte[] excelBytes = orderService.exportOrdersExcel(orderNo, status, startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.xlsx")
                .body(excelBytes);
    }

    public static class OrderCreateDTO {
        private Order order;
        private List<OrderItem> items;
        public Order getOrder() { return order; }
        public void setOrder(Order order) { this.order = order; }
        public List<OrderItem> getItems() { return items; }
        public void setItems(List<OrderItem> items) { this.items = items; }
    }
    public static class StatusDTO {
        private String status;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
