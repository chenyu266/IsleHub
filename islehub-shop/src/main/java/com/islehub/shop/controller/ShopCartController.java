package com.islehub.shop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.islehub.common.result.Result;
import com.islehub.shop.service.CartService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name = "商城-购物车", description = "购物车增删改查")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/shop/cart")
public class ShopCartController {

    private final CartService cartService;

    private Long userId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Operation(summary = "查看购物车")
    @GetMapping
    public Result<List<CartService.CartItem>> list() {
        return Result.ok(cartService.list(userId()));
    }

    @Operation(summary = "加入购物车")
    @PostMapping
    public Result<Void> add(@RequestBody Map<String, Object> body) {
        Long skuId = ((Number) body.get("skuId")).longValue();
        Integer quantity = ((Number) body.get("quantity")).intValue();
        cartService.add(userId(), skuId, quantity);
        return Result.ok();
    }

    @Operation(summary = "修改购物车商品数量")
    @PutMapping("/{skuId}")
    public Result<Void> updateQuantity(@Parameter(description = "SKU ID") @PathVariable Long skuId,
                                       @RequestBody Map<String, Integer> body) {
        cartService.updateQuantity(userId(), skuId, body.get("quantity"));
        return Result.ok();
    }

    @Operation(summary = "从购物车移除")
    @DeleteMapping("/{skuId}")
    public Result<Void> remove(@Parameter(description = "SKU ID") @PathVariable Long skuId) {
        cartService.remove(userId(), skuId);
        return Result.ok();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear(userId());
        return Result.ok();
    }
}
