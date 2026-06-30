package com.islehub.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.islehub.product.entity.ProductSku;
import com.islehub.product.service.ProductService.SkuDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    List<ProductSku> selectByProductIds(@Param("ids") List<Long> ids);

    /** 原子扣减库存（相对更新，无 TOCTOU 问题） */
    @Update("UPDATE product_sku SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    /** 原子恢复库存 */
    @Update("UPDATE product_sku SET stock = stock + #{quantity} WHERE id = #{id}")
    int addStock(@Param("id") Long id, @Param("quantity") int quantity);

    /** 查询 SKU 及关联商品名/主图（一次 JOIN） */
    @Select("SELECT s.id AS skuId, s.product_id AS productId, s.spec, s.price, s.stock, s.status, " +
            "p.name AS productName, p.main_image AS productImage " +
            "FROM product_sku s LEFT JOIN product p ON s.product_id = p.id " +
            "WHERE s.id = #{id}")
    SkuDetail selectSkuDetail(@Param("id") Long id);
}
