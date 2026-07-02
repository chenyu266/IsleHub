package com.islehub.shop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 结算下单请求参数 —— 包含收货地址 ID 和订单备注。
 */
@Data
public class CheckoutDTO {
    @NotNull(message = "地址ID不能为空")
    private Long addressId;

    private String remark;
}
