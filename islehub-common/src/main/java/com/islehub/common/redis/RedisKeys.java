package com.islehub.common.redis;

import java.time.Duration;

/**
 * Redis Key 生成器与 TTL 集中管理
 */
public final class RedisKeys {

    private static final String PREFIX = "IsleHub";

    // ---- TTL ----
    public static final Duration EMAIL_CODE_TTL = Duration.ofSeconds(120);
    public static final Duration EMAIL_RATE_LIMIT_TTL = Duration.ofSeconds(60);
    public static final Duration LOGIN_FAIL_WINDOW = Duration.ofMinutes(2);
    public static final Duration LOGIN_LOCK_TTL = Duration.ofMinutes(2);

    // 商品详情缓存：30 分钟
    public static final Duration PRODUCT_DETAIL_TTL = Duration.ofMinutes(30);
    // 订单详情缓存：10 分钟
    public static final Duration ORDER_DETAIL_TTL = Duration.ofMinutes(10);

    // ---- Key 生成 ----
    public static String emailVerifyCode(String email) {
        return PREFIX + ":verify:email:" + email;
    }

    public static String emailRateLimit(String email) {
        return PREFIX + ":email:ratelimit:" + email;
    }

    public static String loginFail(String username) {
        return PREFIX + ":login:fail:" + username;
    }

    public static String loginLock(String username) {
        return PREFIX + ":login:lock:" + username;
    }

    // ---- 换绑邮箱 ----
    public static String emailChangeCode(String email) {
        return PREFIX + ":email:change:code:" + email;
    }

    public static String emailChangeNewCode(String email) {
        return PREFIX + ":email:change:newcode:" + email;
    }

    public static String emailChangePending(Long userId) {
        return PREFIX + ":email:change:pending:" + userId;
    }

    public static String productDetail(Long id) {
        return PREFIX + ":product:detail:" + id;
    }

    public static String orderDetail(Long id) {
        return PREFIX + ":order:detail:" + id;
    }

    public static String emailCodeAttempts(String email) {
        return PREFIX + ":email:attempts:" + email;
    }
}