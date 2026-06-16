package com.islehub.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.islehub.product.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    List<ProductSku> selectByProductIds(@Param("ids") List<Long> ids);
}
