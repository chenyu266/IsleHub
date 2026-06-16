package com.islehub.shop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.common.result.R;
import com.islehub.product.entity.Product;
import com.islehub.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/product")
public class ShopProductController {

    private final ProductService productService;

    public ShopProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/page")
    public R<R.PageResult<Product>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        Page<Product> result = productService.pageProducts(page, pageSize, keyword, categoryId, 1);
        return R.page(result.getRecords(), result.getTotal(), (long) page, (long) pageSize);
    }

    @GetMapping("/{id}")
    public R<Product> detail(@PathVariable Long id) {
        return R.ok(productService.getDetail(id));
    }
}
