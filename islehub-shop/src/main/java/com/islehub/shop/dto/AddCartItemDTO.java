package com.islehub.shop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加购物车商品请求参数 —— 包含 SKU ID 和数量。
 */
@Data
public class AddCartItemDTO {
    @NotNull(message = "SKU ID 不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;
}
