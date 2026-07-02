package com.islehub.order.dto;

import com.islehub.order.entity.Order;
import com.islehub.order.entity.OrderItem;
import lombok.Data;
import java.util.List;

/**
 * 订单创建请求DTO，封装订单信息及商品明细
 */
@Data
public class OrderCreateDTO {
    private Order order;
    private List<OrderItem> items;
}
