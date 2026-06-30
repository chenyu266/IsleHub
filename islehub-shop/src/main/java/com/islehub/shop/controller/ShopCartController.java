package com.islehub.shop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.islehub.common.result.Result;
import com.islehub.shop.dto.AddCartItemDTO;
import com.islehub.shop.dto.UpdateCartQuantityDTO;
import com.islehub.shop.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    public Result<Void> add(@Valid @RequestBody AddCartItemDTO dto) {
        cartService.add(userId(), dto.getSkuId(), dto.getQuantity());
        return Result.ok();
    }

    @Operation(summary = "修改购物车商品数量")
    @PutMapping("/{skuId}")
    public Result<Void> updateQuantity(@Parameter(description = "SKU ID") @PathVariable Long skuId,
                                       @Valid @RequestBody UpdateCartQuantityDTO dto) {
        cartService.updateQuantity(userId(), skuId, dto.getQuantity());
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
