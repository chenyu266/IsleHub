package com.islehub.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.islehub.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单商品项数据访问层
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
