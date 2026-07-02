package com.islehub.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品数据访问层，提供商品分页查询和详情查询。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    Page<Product> pageProducts(Page<Product> page,
                               @Param("keyword") String keyword,
                               @Param("categoryId") Long categoryId,
                               @Param("status") Integer status);

    Product selectDetailById(@Param("id") Long id);

    Page<Product> pageProductsByCategoryIds(Page<Product> page,
                                            @Param("keyword") String keyword,
                                            @Param("categoryIds") List<Long> categoryIds,
                                            @Param("status") Integer status);
}
