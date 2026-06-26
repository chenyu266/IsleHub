package com.islehub.shop.controller;

import com.islehub.common.result.Result;
import com.islehub.product.entity.Category;
import com.islehub.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/shop/category")
public class ShopCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public Result<List<Category>> tree() {
        return Result.ok(categoryService.getTree());
    }
}
