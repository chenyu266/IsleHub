package com.islehub.product.controller.shop;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.common.result.Result;
import com.islehub.product.entity.Product;
import com.islehub.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商城端商品控制器，提供商品分页查询和详情接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/shop/product")
public class ShopProductController {

    private final ProductService productService;

    @GetMapping("/page")
    public Result<Result.PageResult<Product>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        Page<Product> result = productService.pageProducts(page, pageSize, keyword, categoryId, 1);
        return Result.page(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.ok(productService.getDetail(id));
    }
}
