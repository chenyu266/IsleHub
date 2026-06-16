package com.islehub.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    Page<Order> pageOrders(Page<Order> page,
                           @Param("orderNo") String orderNo,
                           @Param("status") String status,
                           @Param("startDate") String startDate,
                           @Param("endDate") String endDate);

    List<Order> selectForExport(@Param("orderNo") String orderNo,
                                @Param("status") String status,
                                @Param("startDate") String startDate,
                                @Param("endDate") String endDate);

    Order selectDetailById(@Param("id") Long id);
}
