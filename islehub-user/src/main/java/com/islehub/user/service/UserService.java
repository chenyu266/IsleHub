package com.islehub.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.islehub.common.result.Result;
import com.islehub.user.dto.UserAddDTO;
import com.islehub.user.dto.UserRegisterDTO;
import com.islehub.user.dto.UserUpdateDTO;
import com.islehub.user.entity.User;

/**
 * 用户服务接口，定义用户注册、登录、信息管理及管理员用户操作等方法
 */

public interface UserService extends IService<User> {
    Result<String> login(String account, String password);
    void sendEmailCode(String email);
    Page<User> pageUsers(int page, int pageSize, String keyword);
    void addUser(UserAddDTO dto);
    void register(UserRegisterDTO dto);
    void updateUser(Long id, UserUpdateDTO dto);
    void updateStatus(Long id, Integer status);
    void updateUsername(Long userId, String newUsername);
    void updatePassword(Long userId, String oldPassword, String newPassword);
    String updateAvatar(Long userId, org.springframework.web.multipart.MultipartFile file);
    void sendChangeEmailCode(Long userId, String newEmail);
    void verifyOldEmailCode(Long userId, String oldCode);
    void confirmNewEmail(Long userId, String newCode);
}
