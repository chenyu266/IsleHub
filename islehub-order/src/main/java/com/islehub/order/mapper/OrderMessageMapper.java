package com.islehub.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.islehub.order.entity.OrderMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMessageMapper extends BaseMapper<OrderMessage> {

    /** 原子认领待发送消息，返回影响行数（1=认领成功，0=已被其他线程发送） */
    @Update("UPDATE order_message SET status = 1, updated_at = NOW() WHERE order_id = #{orderId} AND status IN (0, 2)")
    int claimForSending(@Param("orderId") Long orderId);
}