package com.islehub.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.islehub.order.entity.OrderShipping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单发货信息数据访问层
 */
@Mapper
public interface OrderShippingMapper extends BaseMapper<OrderShipping> {
}
