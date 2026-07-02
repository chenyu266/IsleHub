package com.islehub.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.islehub.product.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类数据访问层，提供分类表的基础 CRUD 操作。
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
