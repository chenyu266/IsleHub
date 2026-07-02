package com.islehub.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 订单发货信息实体类，映射order_shipping表
 */
@Data
@TableName("order_shipping")
public class OrderShipping {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String carrier;
    private String trackingNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime shippedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deliveredAt;
}
