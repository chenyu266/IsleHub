package com.islehub.order.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 订单消息表实体，用于MQ消息本地表模式，支持发送失败重试
 */
@Data
public class OrderMessage {
    private Long id;
    private Long orderId;
    private String messageBody;
    private Integer status;  // 0-待发送 1-已发送 2-发送失败
    private Integer retryCount;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
