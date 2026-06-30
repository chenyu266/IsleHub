package com.islehub.order.entity;

import lombok.Data;
import java.time.LocalDateTime;

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
