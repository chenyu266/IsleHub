package com.islehub.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOldCodeDTO {
    @NotBlank(message = "验证码不能为空")
    private String oldCode;
}
