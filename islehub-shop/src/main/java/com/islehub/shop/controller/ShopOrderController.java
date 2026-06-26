package com.islehub.shop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.common.result.Result;
import com.islehub.order.entity.Order;
import com.islehub.shop.service.ShopOrderService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Tag(name = "商城-订单", description = "C端订单：下单、查单、取消、确认收货")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/shop/order")
public class ShopOrderController {

    private final ShopOrderService shopOrderService;

    private Long userId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Operation(summary = "提交订单（结算）")
    @PostMapping("/checkout")
    public Result<ShopOrderService.CheckoutResult> checkout(@RequestBody Map<String, Object> body) {
        Long addressId = ((Number) body.get("addressId")).longValue();
        String remark = (String) body.get("remark");
        ShopOrderService.CheckoutResult result = shopOrderService.checkout(userId(), addressId, remark);
        return Result.ok(result);
    }

    @Operation(summary = "分页查询我的订单")
    @GetMapping("/page")
    public Result<Result.PageResult<Order>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        Page<Order> result = shopOrderService.pageOrders(page, pageSize, userId(), status);
        return Result.page(result.getRecords(), result.getTotal(), (long) page, (long) pageSize);
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<Order> detail(@Parameter(description = "订单ID") @PathVariable Long id) {
        return Result.ok(shopOrderService.getDetail(id, userId()));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@Parameter(description = "订单ID") @PathVariable Long id) {
        shopOrderService.cancelOrder(id, userId());
        return Result.ok();
    }

    @Operation(summary = "确认收货")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@Parameter(description = "订单ID") @PathVariable Long id) {
        shopOrderService.confirmOrder(id, userId());
        return Result.ok();
    }
}
