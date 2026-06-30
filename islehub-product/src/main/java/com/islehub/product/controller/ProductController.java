package com.islehub.product.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.islehub.common.result.Result;
import com.islehub.product.dto.BatchStatusDTO;
import com.islehub.product.dto.ProductAddDTO;
import com.islehub.product.entity.Product;
import com.islehub.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@SaCheckRole(value = {"admin", "operator"}, mode = SaMode.OR)
@Tag(name = "管理-商品", description = "商品CRUD、上下架、批量操作")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "分页查询商品")
    @GetMapping("/page")
    public Result<Result.PageResult<Product>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        Page<Product> result = productService.pageProducts(page, pageSize, keyword, categoryId, status);
        return Result.page(result.getRecords(), result.getTotal(), (long) page, (long) pageSize);
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<Product> detail(@Parameter(description = "商品ID") @PathVariable Long id) {
        return Result.ok(productService.getDetail(id));
    }

    @Operation(summary = "新增商品")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody ProductAddDTO dto) {
        productService.addProduct(dto.getProduct(), dto.getSkus());
        return Result.ok();
    }

    @Operation(summary = "编辑商品")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "商品ID") @PathVariable Long id,
                               @Valid @RequestBody ProductAddDTO dto) {
        dto.getProduct().setId(id);
        productService.updateProduct(dto.getProduct(), dto.getSkus());
        return Result.ok();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "商品ID") @PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.ok();
    }

    @Operation(summary = "上下架商品")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "商品ID") @PathVariable Long id,
                                     @Parameter(description = "状态 1=上架 0=下架") @RequestParam Integer status) {
        productService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "批量上下架")
    @PutMapping("/batch-status")
    public Result<Void> batchUpdateStatus(@Valid @RequestBody BatchStatusDTO dto) {
        productService.batchUpdateStatus(dto.getIds(), dto.getStatus());
        return Result.ok();
    }
}
