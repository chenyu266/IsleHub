package com.islehub.shop.controller;

import com.islehub.common.result.R;
import com.islehub.product.entity.Category;
import com.islehub.product.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop/category")
public class ShopCategoryController {

    private final CategoryService categoryService;

    public ShopCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/tree")
    public R<List<Category>> tree() {
        return R.ok(categoryService.getTree());
    }
}
