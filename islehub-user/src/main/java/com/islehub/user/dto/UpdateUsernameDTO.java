package com.islehub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUsernameDTO {
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^\\S+$", message = "用户名不能包含空白字符")
    @Size(min = 1, max = 30, message = "用户名长度不能超过30位")
    private String username;
}
