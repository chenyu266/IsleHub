package com.islehub.config;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpInterface;
import com.islehub.user.entity.User;
import com.islehub.user.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限/角色解析桥接 — 从 DB 读取用户角色供 @SaCheckRole 校验。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    public StpInterfaceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 当前项目只用角色做粗粒度管控，暂不细分权限码
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // Sa-Token 内部将 loginId 存为 String，需要 parse 回 Long
        Long userId = Long.valueOf(loginId.toString());
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new NotPermissionException("账号已被禁用");
        }
        if (user.getRole() == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(user.getRole());
    }
}
