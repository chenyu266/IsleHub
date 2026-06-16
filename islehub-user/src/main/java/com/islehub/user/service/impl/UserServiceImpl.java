package com.islehub.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.islehub.common.exception.BizException;
import com.islehub.user.entity.User;
import com.islehub.user.mapper.UserMapper;
import com.islehub.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public String login(String username, String password) {
        User user = lambdaQuery().eq(User::getUsername, username).one();
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BizException("用户名或密码错误");
        }
        StpUtil.login(user.getId());
        StpUtil.getSession().set("user", user);
        return StpUtil.getTokenValue();
    }

    @Override
    public Page<User> pageUsers(int page, int pageSize, String keyword) {
        return baseMapper.pageUsers(new Page<>(page, pageSize), keyword);
    }

    @Override
    public void addUser(User user) {
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BizException("密码不能为空");
        }
        User exist = lambdaQuery().eq(User::getUsername, user.getUsername()).one();
        if (exist != null) {
            throw new BizException("用户名已存在");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        save(user);
    }

    @Override
    public void register(User user) {
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BizException("密码不能为空");
        }
        User exist = lambdaQuery().eq(User::getUsername, user.getUsername()).one();
        if (exist != null) {
            throw new BizException("用户名已存在");
        }
        user.setRole("customer");
        user.setStatus(1);
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        save(user);
    }

    @Override
    public void updateUser(User user) {
        User exist = lambdaQuery().eq(User::getUsername, user.getUsername())
                .ne(User::getId, user.getId()).one();
        if (exist != null) {
            throw new BizException("用户名已存在");
        }
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(BCrypt.hashpw(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        updateById(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        updateById(user);
    }
}
