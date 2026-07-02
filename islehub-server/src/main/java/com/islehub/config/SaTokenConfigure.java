package com.islehub.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 鉴权拦截器配置 — 拦截 /api/** 请求进行登录校验及用户状态检查。
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private final UserStatusChecker statusChecker;

    public SaTokenConfigure(UserStatusChecker statusChecker) {
        this.statusChecker = statusChecker;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            StpUtil.checkLogin();
            statusChecker.checkEnabled(StpUtil.getLoginIdAsLong());
        }))
        .addPathPatterns("/api/**")
        .excludePathPatterns(
                "/api/user/login",
                "/api/user/register",
                "/api/user/send-email-code",
                "/api/shop/product/**",
                "/api/shop/category/**"
        );
    }
}
