package com.islehub.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "商品名称不能为空")
    private String name;
    private Long categoryId;
    private String description;
    private String mainImage;
    private String images;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private java.util.List<ProductSku> skus;
}
