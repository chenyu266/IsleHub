package com.islehub.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "请输入正确的邮箱格式")
    @Size(max = 200, message = "邮箱长度不能超过200个字符")
    private String email;

    @Size(min = 6, max = 6, message = "验证码长度是6位的")
    @NotBlank(message = "验证码不能为空")
    private String emailConfirmCode;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位")
    @Pattern(regexp = ".*[a-zA-Z].*", message = "密码必须包含英文字母")
    @Pattern(regexp = ".*\\d.*", message = "密码必须包含数字")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
