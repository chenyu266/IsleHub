package com.islehub.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.islehub.user.entity.User;

public interface UserService extends IService<User> {
    String login(String username, String password);
    Page<User> pageUsers(int page, int pageSize, String keyword);
    void addUser(User user);
    void register(User user);
    void updateUser(User user);
    void updateStatus(Long id, Integer status);
}
