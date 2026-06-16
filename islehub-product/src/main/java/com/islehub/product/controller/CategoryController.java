package com.islehub.product.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.islehub.common.result.R;
import com.islehub.product.entity.Category;
import com.islehub.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@SaCheckRole(value = {"admin", "operator"}, mode = SaMode.OR)
@Tag(name = "管理-分类", description = "商品分类树、CRUD")
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public R<List<Category>> tree() {
        return R.ok(categoryService.getTree());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public R<Void> add(@Valid @RequestBody Category category) {
        categoryService.save(category);
        return R.ok();
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "分类ID") @PathVariable Long id,
                          @Valid @RequestBody Category category) {
        category.setId(id);
        categoryService.updateById(category);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "分类ID") @PathVariable Long id) {
        categoryService.removeById(id);
        return R.ok();
    }
}
