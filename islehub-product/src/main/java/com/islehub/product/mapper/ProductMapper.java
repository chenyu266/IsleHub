package com.islehub.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    Page<Product> pageProducts(Page<Product> page,
                               @Param("keyword") String keyword,
                               @Param("categoryId") Long categoryId,
                               @Param("status") Integer status);

    Product selectDetailById(@Param("id") Long id);
}
