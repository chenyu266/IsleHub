package com.islehub.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.islehub.product.entity.Category;
import java.util.List;

public interface CategoryService extends IService<Category> {
    List<Category> getTree();
    List<Long> getCategoryAndChildrenIds(Long categoryId);
    List<Category> listLeaves();
    Category getDefaultCategory();
    List<Category> getShopTree();
}
