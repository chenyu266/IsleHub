package com.islehub.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 验证旧邮箱验证码请求参数，用于换绑邮箱流程中校验用户身份
 */

@Data
public class VerifyOldCodeDTO {
    @NotBlank(message = "验证码不能为空")
    private String oldCode;
}
