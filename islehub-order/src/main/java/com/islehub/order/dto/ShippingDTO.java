package com.islehub.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShippingDTO {
    @NotBlank(message = "快递公司不能为空")
    private String carrier;

    @NotBlank(message = "快递单号不能为空")
    private String trackingNo;
}
