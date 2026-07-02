package com.islehub.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 管理员编辑用户信息请求参数，支持修改邮箱、用户名、昵称、手机号等字段
 */

@Data
public class UserUpdateDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    private String username;

    private String nickname;

    private String phone;

    private String avatar;

    private Integer gender;

    private LocalDate birthday;

    private String signature;

    private Integer roleType;

    private Integer status;
}
