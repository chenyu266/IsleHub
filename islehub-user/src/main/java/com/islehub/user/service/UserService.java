package com.islehub.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.islehub.common.result.Result;
import com.islehub.user.dto.UserAddDTO;
import com.islehub.user.dto.UserRegisterDTO;
import com.islehub.user.dto.UserUpdateDTO;
import com.islehub.user.entity.User;

public interface UserService extends IService<User> {
    Result<String> login(String account, String password);
    void sendEmailCode(String email);
    Page<User> pageUsers(int page, int pageSize, String keyword);
    void addUser(UserAddDTO dto);
    void register(UserRegisterDTO dto);
    void updateUser(Long id, UserUpdateDTO dto);
    void updateStatus(Long id, Integer status);
}
