package com.islehub.product.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.islehub.common.result.Result;
import com.islehub.product.entity.Category;
import com.islehub.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 管理端分类控制器，处理分类的增删改查及分类树查询。
 */
@SaCheckRole(value = {"admin", "operator"}, mode = SaMode.OR)
@Tag(name = "管理-分类", description = "商品分类树、CRUD")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public Result<List<Category>> tree() {
        return Result.ok(categoryService.getTree());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Category category) {
        categoryService.save(category);
        return Result.ok();
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "分类ID") @PathVariable Long id,
                               @Valid @RequestBody Category category) {
        category.setId(id);
        categoryService.updateById(category);
        return Result.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "分类ID") @PathVariable Long id) {
        categoryService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "获取三级叶子分类")
    @GetMapping("/leaves")
    public Result<List<Category>> leaves() {
        return Result.ok(categoryService.listLeaves());
    }
}
