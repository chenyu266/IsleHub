package com.islehub.order.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.common.result.Result;
import com.islehub.order.dto.OrderCreateDTO;
import com.islehub.order.dto.ShippingDTO;
import com.islehub.order.dto.StatusDTO;
import com.islehub.order.entity.Order;
import com.islehub.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理控制器，提供订单的增删改查、状态流转及Excel导出接口
 */
@SaCheckRole(value = {"admin", "operator"}, mode = SaMode.OR)
@Tag(name = "管理-订单", description = "订单CRUD、状态流转、导出")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "分页查询订单")
    @GetMapping("/page")
    public Result<Result.PageResult<Order>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<Order> result = orderService.pageOrders(page, pageSize, orderNo, status, startDate, endDate);
        return Result.page(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<Order> detail(@Parameter(description = "订单ID") @PathVariable Long id) {
        return Result.ok(orderService.getDetail(id));
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody OrderCreateDTO dto) {
        orderService.createOrder(dto.getOrder(), dto.getItems());
        return Result.ok();
    }

    @Operation(summary = "更新订单状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "订单ID") @PathVariable Long id,
                                     @Valid @RequestBody StatusDTO dto) {
        orderService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@Parameter(description = "订单ID") @PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.ok();
    }

    @Operation(summary = "订单发货")
    @PostMapping("/{id}/shipping")
    public Result<Void> shipping(@Parameter(description = "订单ID") @PathVariable Long id,
                                 @Valid @RequestBody ShippingDTO dto) {
        orderService.addShipping(id, dto.getCarrier(), dto.getTrackingNo());
        return Result.ok();
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
}
