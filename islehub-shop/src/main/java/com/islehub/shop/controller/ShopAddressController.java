package com.islehub.shop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.islehub.common.result.R;
import com.islehub.shop.entity.Address;
import com.islehub.shop.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "商城-地址", description = "收货地址增删改查")
@RestController
@RequestMapping("/api/shop/address")
public class ShopAddressController {

    private final AddressService addressService;

    public ShopAddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    private Long userId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Operation(summary = "查询收货地址列表")
    @GetMapping
    public R<List<Address>> list() {
        return R.ok(addressService.listByUser(userId()));
    }

    @Operation(summary = "新增收货地址")
    @PostMapping
    public R<Void> add(@Valid @RequestBody Address address) {
        addressService.addAddress(address, userId());
        return R.ok();
    }

    @Operation(summary = "编辑收货地址")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "地址ID") @PathVariable Long id,
                          @Valid @RequestBody Address address) {
        address.setId(id);
        addressService.updateAddress(address, userId());
        return R.ok();
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "地址ID") @PathVariable Long id) {
        addressService.deleteAddress(id, userId());
        return R.ok();
    }
}
