package com.islehub.shop.controller;

import com.islehub.common.result.R;
import com.islehub.user.entity.User;
import com.islehub.user.service.UserService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商城-认证", description = "注册（登录和信息复用 /api/user 接口）")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/shop/auth")
public class ShopAuthController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody User user) {
        userService.register(user);
        return R.ok();
    }
}
