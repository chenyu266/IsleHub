package com.islehub.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmNewCodeDTO {
    @NotBlank(message = "验证码不能为空")
    private String newCode;
}
