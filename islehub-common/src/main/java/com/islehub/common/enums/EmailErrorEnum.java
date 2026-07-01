package com.islehub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmailErrorEnum {

    INVALID_ADDRESS("invalid address", "邮箱地址格式错误或不存在"),
    UNKNOWN_USER("unknown user", "邮箱地址不存在"),
    AUTH_FAILED("authentication failed", "发件邮箱认证失败（检查授权码）"),
    ERROR_535("535", "发件邮箱认证失败（检查授权码）"),
    CONNECTION_TIMEOUT("connection timed out", "邮件服务器连接超时，请稍后重试"),
    COULD_NOT_CONNECT("could not connect", "邮件服务器连接失败"),
    REJECTED("message rejected", "邮件被对方服务器拒收"),
    REFUSED("refused", "邮件被拒收"),
    TIMEOUT("timeout", "邮件发送超时，请重试");

    private final String keyword;
    private final String message;
}