package com.islehub.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.islehub.shop.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货地址数据访问层 —— 基于 MyBatis-Plus 的 Mapper 接口。
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
