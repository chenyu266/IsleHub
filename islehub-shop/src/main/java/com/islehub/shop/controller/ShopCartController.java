package com.islehub.shop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.islehub.common.result.R;
import com.islehub.shop.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name = "商城-购物车", description = "购物车增删改查")
@RestController
@RequestMapping("/api/shop/cart")
public class ShopCartController {

    private final CartService cartService;

    public ShopCartController(CartService cartService) {
        this.cartService = cartService;
    }

    private Long userId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Operation(summary = "查看购物车")
    @GetMapping
    public R<List<CartService.CartItem>> list() {
        return R.ok(cartService.list(userId()));
    }

    @Operation(summary = "加入购物车")
    @PostMapping
    public R<Void> add(@RequestBody Map<String, Object> body) {
        Long skuId = ((Number) body.get("skuId")).longValue();
        Integer quantity = ((Number) body.get("quantity")).intValue();
        cartService.add(userId(), skuId, quantity);
        return R.ok();
    }

    @Operation(summary = "修改购物车商品数量")
    @PutMapping("/{skuId}")
    public R<Void> updateQuantity(@Parameter(description = "SKU ID") @PathVariable Long skuId,
                                  @RequestBody Map<String, Integer> body) {
        cartService.updateQuantity(userId(), skuId, body.get("quantity"));
        return R.ok();
    }

    @Operation(summary = "从购物车移除")
    @DeleteMapping("/{skuId}")
    public R<Void> remove(@Parameter(description = "SKU ID") @PathVariable Long skuId) {
        cartService.remove(userId(), skuId);
        return R.ok();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public R<Void> clear() {
        cartService.clear(userId());
        return R.ok();
    }
}
