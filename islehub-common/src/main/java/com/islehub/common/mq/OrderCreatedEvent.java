package com.islehub.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

/**
 * 订单创建事件：订单创建后发送到消息队列的事件体，包含订单 ID 及扣减库存所需的商品明细
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private List<OrderItemStock> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemStock implements Serializable {
        private Long skuId;
        private int quantity;
    }
}
