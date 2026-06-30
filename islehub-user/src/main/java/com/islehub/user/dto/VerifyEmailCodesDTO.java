package com.islehub.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import jakarta.validation.constraints.Email;

@Data
public class VerifyEmailCodesDTO {
    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String newEmail;

    @NotBlank(message = "旧邮箱验证码不能为空")
    private String oldCode;

    @NotBlank(message = "新邮箱验证码不能为空")
    private String newCode;
}
