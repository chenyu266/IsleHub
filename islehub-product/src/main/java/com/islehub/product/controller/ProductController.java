package com.islehub.product.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.common.result.R;
import com.islehub.product.entity.Product;
import com.islehub.product.entity.ProductSku;
import com.islehub.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@SaCheckRole(value = {"admin", "operator"}, mode = SaMode.OR)
@Tag(name = "管理-商品", description = "商品CRUD、上下架、批量操作")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "分页查询商品")
    @GetMapping("/page")
    public R<R.PageResult<Product>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        Page<Product> result = productService.pageProducts(page, pageSize, keyword, categoryId, status);
        return R.page(result.getRecords(), result.getTotal(), (long) page, (long) pageSize);
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public R<Product> detail(@Parameter(description = "商品ID") @PathVariable Long id) {
        return R.ok(productService.getDetail(id));
    }

    @Operation(summary = "新增商品")
    @PostMapping
    public R<Void> add(@Valid @RequestBody ProductAddDTO dto) {
        productService.addProduct(dto.getProduct(), dto.getSkus());
        return R.ok();
    }

    @Operation(summary = "编辑商品")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "商品ID") @PathVariable Long id,
                          @Valid @RequestBody ProductAddDTO dto) {
        dto.getProduct().setId(id);
        productService.updateProduct(dto.getProduct(), dto.getSkus());
        return R.ok();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "商品ID") @PathVariable Long id) {
        productService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "上下架商品")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@Parameter(description = "商品ID") @PathVariable Long id,
                                @Parameter(description = "状态 1=上架 0=下架") @RequestParam Integer status) {
        productService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "批量上下架")
    @PutMapping("/batch-status")
    public R<Void> batchUpdateStatus(@Valid @RequestBody BatchStatusDTO dto) {
        productService.batchUpdateStatus(dto.getIds(), dto.getStatus());
        return R.ok();
    }

    // Inner DTOs — must be public static for Jackson deserialization
    public static class ProductAddDTO {
        private Product product;
        private List<ProductSku> skus;
        public Product getProduct() { return product; }
        public void setProduct(Product product) { this.product = product; }
        public List<ProductSku> getSkus() { return skus; }
        public void setSkus(List<ProductSku> skus) { this.skus = skus; }
    }
    public static class BatchStatusDTO {
        private List<Long> ids;
        private Integer status;
        public List<Long> getIds() { return ids; }
        public void setIds(List<Long> ids) { this.ids = ids; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}
