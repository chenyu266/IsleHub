package com.islehub.shop.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.islehub.common.result.Result;
import com.islehub.shop.entity.Address;
import com.islehub.shop.service.AddressService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * C 端收货地址控制器 —— 提供收货地址的增删改查接口。
 */
@SaCheckLogin
@Tag(name = "商城-地址", description = "收货地址增删改查")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/shop/address")
public class ShopAddressController {

    private final AddressService addressService;

    private Long userId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Operation(summary = "查询收货地址列表")
    @GetMapping
    public Result<List<Address>> list() {
        return Result.ok(addressService.listByUser(userId()));
    }

    @Operation(summary = "新增收货地址")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Address address) {
        addressService.addAddress(address, userId());
        return Result.ok();
    }

    @Operation(summary = "编辑收货地址")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "地址ID") @PathVariable Long id,
                               @Valid @RequestBody Address address) {
        address.setId(id);
        addressService.updateAddress(address, userId());
        return Result.ok();
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "地址ID") @PathVariable Long id) {
        addressService.deleteAddress(id, userId());
        return Result.ok();
    }
}
