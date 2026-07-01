package com.islehub.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一响应状态码枚举
 * HTTP 状态码段（与标准 HTTP 状态码一致）：
 *   2xx — 成功
 *   4xx — 客户端错误
 *   5xx — 服务端错误
 */
@Getter
public enum RCode {

    // ==================== 2xx 成功 ====================
    OK(200, "操作成功"),
    CREATED(201, "创建成功"),

    // ==================== 4xx 客户端错误 ====================
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突，已存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后重试"),

    // ==================== 5xx 服务端错误 ====================
    INTERNAL_ERROR(500, "服务器内部错误"),
    BAD_GATEWAY(502, "网关错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用，请稍后重试"),

    ;

    private final int code;
    private final String message;

    RCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private static final Map<Integer, RCode> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(RCode::getCode, e -> e, (a, b) -> a));

    /**
     * 根据状态码查找枚举，找不到返回 {@code null}
     */
    public static RCode of(Integer code) {
        return CODE_MAP.get(code);
    }

    /**
     * 判断是否为成功状态
     */
    public boolean isSuccess() {
        return this.code >= 200 && this.code < 300;
    }
}
