package com.islehub.user.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.common.result.R;
import com.islehub.user.entity.User;
import com.islehub.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理-用户", description = "用户CRUD、登录、状态管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<String> login(@RequestBody User user) {
        String token = userService.login(user.getUsername(), user.getPassword());
        return R.ok(token);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public R<User> info() {
        User user = (User) StpUtil.getSession().get("user");
        user.setPassword(null);
        return R.ok(user);
    }

    @SaCheckRole("admin")
    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public R<R.PageResult<User>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        Page<User> result = userService.pageUsers(page, pageSize, keyword);
        return R.page(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @SaCheckRole("admin")
    @Operation(summary = "新增用户")
    @PostMapping
    public R<Void> add(@Valid @RequestBody User user) {
        userService.addUser(user);
        return R.ok();
    }

    @SaCheckRole("admin")
    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "用户ID") @PathVariable Long id,
                          @Valid @RequestBody User user) {
        user.setId(id);
        userService.updateUser(user);
        return R.ok();
    }

    @SaCheckRole("admin")
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "用户ID") @PathVariable Long id) {
        userService.removeById(id);
        return R.ok();
    }

    @SaCheckRole("admin")
    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@Parameter(description = "用户ID") @PathVariable Long id,
                                @Parameter(description = "状态 1=启用 0=禁用") @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return R.ok();
    }
}
