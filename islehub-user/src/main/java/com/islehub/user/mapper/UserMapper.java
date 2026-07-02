package com.islehub.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户数据访问层，提供用户分页查询等数据库操作
 */

@Mapper
public interface UserMapper extends BaseMapper<User> {

    Page<User> pageUsers(Page<User> page, @Param("keyword") String keyword);
}
