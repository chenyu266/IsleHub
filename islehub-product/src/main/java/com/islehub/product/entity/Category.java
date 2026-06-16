package com.islehub.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "分类名称不能为空")
    private String name;
    private Long parentId;
    private Integer sortOrder;
    @TableField(exist = false)
    private List<Category> children;
}
