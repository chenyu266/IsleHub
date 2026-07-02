package com.islehub.common.result;

import com.islehub.common.enums.RCode;
import lombok.*;

/**
 * 统一 API 响应包装类：封装状态码、消息和数据，提供 ok/fail/page 静态工厂方法简化 Controller 层返回
 */
@Getter
@ToString(exclude = "data")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Result<T> {
    private final int code;
    private final String message;
    private final T data;

    public static <T> Result<T> ok() {
        return new Result<>(RCode.OK.getCode(), RCode.OK.getMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(RCode.OK.getCode(), RCode.OK.getMessage(), data);
    }

    public static <T> Result<T> ok(String message, T data) { return new Result<>(RCode.OK.getCode(), message, data); }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(RCode rCode) {
        return new Result<>(rCode.getCode(), rCode.getMessage(), null);
    }

    public static <T> Result<T> fail(RCode rCode, String message) {
        return new Result<>(rCode.getCode(), message, null);
    }

    public static <T> Result<T> paramError(String message) {
        return new Result<>(RCode.BAD_REQUEST.getCode(), message, null);
    }

    public static <T> Result<PageResult<T>> page(java.util.List<T> records, long total, long page, long pageSize) {
        return ok(new PageResult<>(records, total, page, pageSize));
    }

    @Getter
    public static class PageResult<T> {
        private final java.util.List<T> records;
        private final long total;
        private final long page;
        private final long pageSize;

        public PageResult(java.util.List<T> records, long total, long page, long pageSize) {
            this.records = records;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }
    }
}
