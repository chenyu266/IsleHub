package com.islehub.config;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.islehub.user.entity.User;
import com.islehub.user.mapper.UserMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private final UserMapper userMapper;

    public SaTokenConfigure(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            StpUtil.checkLogin();
            Long userId = Long.valueOf(StpUtil.getLoginId().toString());
            User user = userMapper.selectById(userId);
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                StpUtil.logout();
                throw new NotPermissionException("账号已被禁用");
            }
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
