package com.islehub.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.islehub.product.entity.Product;
import com.islehub.product.entity.ProductSku;
import java.util.List;

public interface ProductService extends IService<Product> {
    Page<Product> pageProducts(int page, int pageSize, String keyword, Long categoryId, Integer status);
    Product getDetail(Long id);
    void addProduct(Product product, List<ProductSku> skus);
    void updateProduct(Product product, List<ProductSku> skus);
    void updateStatus(Long id, Integer status);
    void batchUpdateStatus(List<Long> ids, Integer status);
}
