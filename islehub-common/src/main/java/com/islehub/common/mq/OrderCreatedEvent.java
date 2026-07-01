package com.islehub.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

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
