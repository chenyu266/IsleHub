package com.islehub.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 邮箱换绑请求参数，包含用户要绑定的新邮箱地址
 */

@Data
public class ChangeEmailDTO {
    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String newEmail;
}
