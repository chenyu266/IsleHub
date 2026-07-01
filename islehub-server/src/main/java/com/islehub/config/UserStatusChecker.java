package com.islehub.config;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.islehub.user.entity.User;
import com.islehub.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户状态校验 — 供 SaTokenConfigure 等使用，避免配置类直接依赖 Mapper。
 */
@Component
@RequiredArgsConstructor
public class UserStatusChecker {

    private final UserMapper userMapper;

    public void checkEnabled(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            StpUtil.logout();
            throw new NotPermissionException("账号已被禁用");
        }
    }
}
