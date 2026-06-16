package com.islehub.shop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.islehub.common.result.R;
import com.islehub.user.entity.User;
import com.islehub.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商城-认证", description = "注册、登录、获取用户信息")
@RestController
@RequestMapping("/api/shop/auth")
public class ShopAuthController {

    private final UserService userService;

    public ShopAuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody User user) {
        userService.register(user);
        return R.ok();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<String> login(@RequestBody User user) {
        String token = userService.login(user.getUsername(), user.getPassword());
        return R.ok(token);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/info")
    public R<User> info() {
        User user = (User) StpUtil.getSession().get("user");
        user.setPassword(null);
        return R.ok(user);
    }
}
