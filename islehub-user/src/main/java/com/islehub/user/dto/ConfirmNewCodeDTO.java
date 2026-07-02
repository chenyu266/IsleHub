package com.islehub.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 确认新邮箱验证码请求参数，用于完成邮箱换绑的最后一步校验
 */

@Data
public class ConfirmNewCodeDTO {
    @NotBlank(message = "验证码不能为空")
    private String newCode;
}
