package com.islehub.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.islehub.product.entity.Product;
import com.islehub.product.entity.ProductSku;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService extends IService<Product> {
    Page<Product> pageProducts(int page, int pageSize, String keyword, Long categoryId, Integer status);
    Product getDetail(Long id);
    void addProduct(Product product, List<ProductSku> skus);
    void updateProduct(Product product, List<ProductSku> skus);
    void updateStatus(Long id, Integer status);
    void batchUpdateStatus(List<Long> ids, Integer status);
    void deleteProduct(Long id);

    // ---- SKU 查询（供跨模块调用） ----
    /** 按 ID 查 SKU，返回 null 表示不存在 */
    ProductSku getSkuById(Long id);

    /** 恢复 SKU 库存（取消订单时调用） */
    void addSkuStock(Long skuId, int quantity);

    /** 批量按 productId 查 SKU */
    List<ProductSku> getSkusByProductIds(List<Long> productIds);

    /** 查 SKU + 商品名/主图（一次 JOIN，供加购等高频场景） */
    SkuDetail getSkuDetail(Long skuId);

    @Data
    class SkuDetail {
        private Long skuId;
        private Long productId;
        private String spec;
        private BigDecimal price;
        private Integer stock;
        private Integer status;
        private String productName;
        private String productImage;
    }
}
