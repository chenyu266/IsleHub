package com.islehub.order.dto;

import com.islehub.order.entity.Order;
import com.islehub.order.entity.OrderItem;
import lombok.Data;
import java.util.List;

@Data
public class OrderCreateDTO {
    private Order order;
    private List<OrderItem> items;
}
