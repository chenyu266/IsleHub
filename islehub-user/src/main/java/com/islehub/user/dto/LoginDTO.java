package com.islehub.user.dto;

import lombok.Data;

/**
 * 用户登录请求参数，支持使用邮箱或用户名进行登录
 */

@Data
public class LoginDTO {
    private String account;
    private String password;
}
