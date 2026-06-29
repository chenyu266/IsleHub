package com.islehub.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @NotBlank(message = "用户名不能为空")
    private String username;
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    @NotBlank(message = "密码不能为空")
    private String password;
    @NotBlank(message = "邮箱不能为空")
    private String email;
    private String phone;
    private String avatar;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    @TableLogic
    private Integer deleteFlag;
    private String role;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
