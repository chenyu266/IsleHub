package com.islehub.common.result;

import com.islehub.common.enums.RCode;
import lombok.*;

@Getter
@ToString(exclude = "data")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class R<T> {
    private final int code;
    private final String message;
    private final T data;

    public static <T> R<T> ok() {
        return new R<>(RCode.OK.getCode(), RCode.OK.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(RCode.OK.getCode(), RCode.OK.getMessage(), data);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(RCode rCode) {
        return new R<>(rCode.getCode(), rCode.getMessage(), null);
    }

    public static <T> R<T> fail(RCode rCode, String message) {
        return new R<>(rCode.getCode(), message, null);
    }

    public static <T> R<T> paramError(String message) {
        return new R<>(RCode.BAD_REQUEST.getCode(), message, null);
    }

    public static <T> R<PageResult<T>> page(java.util.List<T> records, long total, long page, long pageSize) {
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
