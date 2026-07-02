package com.islehub.product.dto;

import com.islehub.product.entity.Product;
import com.islehub.product.entity.ProductSku;
import lombok.Data;
import java.util.List;

/**
 * 商品新增请求 DTO，包含商品信息和 SKU 列表。
 */
@Data
public class ProductAddDTO {
    private Product product;
    private List<ProductSku> skus;
}
