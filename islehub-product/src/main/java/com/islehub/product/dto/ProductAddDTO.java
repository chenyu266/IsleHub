package com.islehub.product.dto;

import com.islehub.product.entity.Product;
import com.islehub.product.entity.ProductSku;
import lombok.Data;
import java.util.List;

@Data
public class ProductAddDTO {
    private Product product;
    private List<ProductSku> skus;
}
