package com.islehub.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.common.result.Result;
import com.islehub.user.dto.ChangeEmailDTO;
import com.islehub.user.dto.ConfirmNewCodeDTO;
import com.islehub.user.dto.LoginDTO;
import com.islehub.user.dto.UpdatePasswordDTO;
import com.islehub.user.dto.UpdateUsernameDTO;
import com.islehub.user.dto.UserAddDTO;
import com.islehub.user.dto.UserRegisterDTO;
import com.islehub.user.dto.UserUpdateDTO;
import com.islehub.user.dto.VerifyOldCodeDTO;
import com.islehub.user.entity.User;
import com.islehub.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@SaCheckLogin
@Tag(name = "管理-用户", description = "用户CRUD、登录、状态管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @SaIgnore
    @Operation(summary = "登录（支持邮箱或用户名）")
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO dto) {
        return userService.login(dto.getAccount(), dto.getPassword());
    }

    @SaIgnore
    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/send-email-code")
    public Result<Void> sendEmailCode(@RequestParam String email) {
        userService.sendEmailCode(email);
        return Result.ok();
    }

    @SaIgnore
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
        userService.register(dto);
        return Result.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<User> info() {
        User user = (User) StpUtil.getSession().get("user");
        user.setPassword(null);
        return Result.ok(user);
    }

    @Operation(summary = "修改用户名")
    @PutMapping("/username")
    public Result<Void> updateUsername(@Valid @RequestBody UpdateUsernameDTO dto) {
        userService.updateUsername(StpUtil.getLoginIdAsLong(), dto.getUsername());
        return Result.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        userService.updatePassword(StpUtil.getLoginIdAsLong(), dto.getOldPassword(), dto.getNewPassword());
        return Result.ok();
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> updateAvatar(@RequestParam("file") MultipartFile file) {
        String url = userService.updateAvatar(StpUtil.getLoginIdAsLong(), file);
        return Result.ok(url);
    }

    @Operation(summary = "发起换绑 — 向当前邮箱发送验证码")
    @PostMapping("/email/change")
    public Result<Void> sendChangeEmailCode() {
        userService.sendChangeEmailCode(StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    @Operation(summary = "验证旧邮箱 — 校验身份")
    @PostMapping("/email/verify")
    public Result<Void> verifyOldCode(@Valid @RequestBody VerifyOldCodeDTO dto) {
        userService.verifyOldEmailCode(StpUtil.getLoginIdAsLong(), dto.getOldCode());
        return Result.ok();
    }

    @Operation(summary = "提交新邮箱 — 需先通过旧邮箱验证，向新邮箱发送验证码")
    @PostMapping("/email/new")
    public Result<Void> setNewEmail(@Valid @RequestBody ChangeEmailDTO dto) {
        userService.setNewEmail(StpUtil.getLoginIdAsLong(), dto.getNewEmail());
        return Result.ok();
    }

    @Operation(summary = "确认新邮箱 — 验证新邮箱验证码并完成换绑")
    @PutMapping("/email")
    public Result<Void> confirmNewEmail(@Valid @RequestBody ConfirmNewCodeDTO dto) {
        userService.confirmNewEmail(StpUtil.getLoginIdAsLong(), dto.getNewCode());
        return Result.ok();
    }

    @SaCheckRole("admin")
    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<Result.PageResult<User>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        Page<User> result = userService.pageUsers(page, pageSize, keyword);
        return Result.page(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @SaCheckRole("admin")
    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody UserAddDTO dto) {
        userService.addUser(dto);
        return Result.ok();
    }

    @SaCheckRole("admin")
    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "用户ID") @PathVariable Long id,
                               @Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(id, dto);
        return Result.ok();
    }

    @SaCheckRole("admin")
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "用户ID") @PathVariable Long id) {
        userService.removeById(id);
        return Result.ok();
    }

    @SaCheckRole("admin")
    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "用户ID") @PathVariable Long id,
                                     @Parameter(description = "状态 1=启用 0=禁用") @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }
}
