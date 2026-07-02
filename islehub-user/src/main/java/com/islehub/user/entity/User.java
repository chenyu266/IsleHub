package com.islehub.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类，映射数据库user表，包含用户基本信息、登录记录及软删除标记
 */

@Data
@TableName("`user`")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @NotBlank(message = "用户名不能为空")
    private String username;
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    @NotBlank(message = "密码不能为空")
    @JsonIgnore
    private String password;
    @NotBlank(message = "邮箱不能为空")
    private String email;
    private String phone;
    private String avatar;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    @TableLogic(value = "0", delval = "1")
    private Integer deleteFlag;
    private String role;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
