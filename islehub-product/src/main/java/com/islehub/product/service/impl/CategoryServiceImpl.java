package com.islehub.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islehub.common.exception.BizException;
import com.islehub.product.entity.Category;
import com.islehub.product.entity.Product;
import com.islehub.product.mapper.CategoryMapper;
import com.islehub.product.mapper.ProductMapper;
import com.islehub.product.service.CategoryService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类服务实现类，支持分类树缓存和事务操作。
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private static final String CACHE_KEY = "category:tree";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryServiceImpl(StringRedisTemplate redisTemplate ,ProductMapper productMapper) {
        this.redisTemplate = redisTemplate;
        this.productMapper = productMapper;
    }

    @Override
    public List<Category> getTree() {
        // 1. 查缓存
        String cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<Category>>() {});
            } catch (JsonProcessingException e) {
                // 缓存数据损坏，忽略并回源查库
            }
        }

        // 2. 查库 + 内存构建树
        List<Category> level1 = listByLevel(1);
        List<Category> level2 = listByLevel(2);
        List<Category> level3 = listByLevel(3);


        Map<Long, List<Category>> level2Map = level2.stream().collect(Collectors.groupingBy(Category::getParentId));
        Map<Long, List<Category>> level3Map = level3.stream().collect(Collectors.groupingBy(Category::getParentId));

        level2.forEach(l2 -> l2.setChildren(level3Map.getOrDefault(l2.getId(), List.of())));
        level1.forEach(l1 -> l1.setChildren(level2Map.getOrDefault(l1.getId(), List.of())));

        // 3. 写缓存
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(level1), CACHE_TTL);
        } catch (JsonProcessingException ignored) {
        }

        return level1;
    }

    @Override
    public List<Long> getCategoryAndChildrenIds(Long categoryId){
        if (categoryId ==null||categoryId<=0){
            return List.of();
        }
        Category category=getById(categoryId);
        if (category==null){
            return List.of();
        }

        List<Long> ids=new ArrayList<>();
        ids.add(categoryId);

        if(category.getLevel()==1){
            List<Category> l2=listByParent(categoryId);
            for (Category c2:l2){
                ids.add(c2.getId());
                ids.addAll(listByParent(c2.getId()).stream().map(Category::getId).toList());
            }
        }else if (category.getLevel() == 2) {
            ids.addAll(listByParent(categoryId).stream().map(Category::getId).toList());
        }
        return ids;

    }

    @Override
    public List<Category> listLeaves() {
        return listByLevel(3);
    }

    private List<Category> listByLevel(int level) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getLevel, level)
                        .orderByAsc(Category::getId));
    }

    private List<Category> listByParent(Long parentId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, parentId)
                        .orderByAsc(Category::getId));
    }

    @Override
    public boolean save(Category entity) {
        computeLevel(entity);
        validateParentCanHaveChildren(entity.getParentId());
        boolean result = super.save(entity);
        redisTemplate.delete(CACHE_KEY);
        return result;
    }

    @Override
    public boolean updateById(Category entity) {
        Category existing = getById(entity.getId());
        if (existing == null) {
            throw new BizException("分类不存在");
        }

        if (entity.getParentId() != null && !entity.getParentId().equals(existing.getParentId())) {
            computeLevel(entity);
            validateParentCanHaveChildren(entity.getParentId());
            validateNotDescendant(entity.getId(), entity.getParentId());
        } else {
            entity.setLevel(existing.getLevel());
        }

        boolean result = super.updateById(entity);
        redisTemplate.delete(CACHE_KEY);
        return result;
    }

    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        Category category = getById(id);
        if (category == null) {
            return false;
        }

        // 默认分类不能删
        if (Integer.valueOf(1).equals(category.getIsDefault())) {
            throw new BizException("默认分类不能删除");
        }

        // 有子分类不能删
        long childCount = baseMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, category.getId()));
        if (childCount > 0) {
            throw new BizException("该分类下存在子分类，无法删除");
        }

        // 把该分类下的商品迁移到默认分类
        Category defaultCat = getDefaultCategory();
        productMapper.update(null,
                new LambdaUpdateWrapper<Product>()
                        .set(Product::getCategoryId, defaultCat.getId())
                        .eq(Product::getCategoryId, category.getId()));

        boolean result = super.removeById(id);
        redisTemplate.delete(CACHE_KEY);
        return result;
    }

    private void computeLevel(Category entity) {
        Long parentId = entity.getParentId() == null ? 0L : entity.getParentId();
        if (parentId == 0L) {
            entity.setParentId(0L);
            entity.setLevel(1);
        } else {
            Category parent = getById(parentId);
            if (parent == null) {
                throw new BizException("父分类不存在");
            }
            int level = parent.getLevel() + 1;
            if (level > 3) {
                throw new BizException("最多支持三级分类");
            }
            entity.setLevel(level);
        }
    }

    private void validateParentCanHaveChildren(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        Category parent = getById(parentId);
        if (parent == null) {
            throw new BizException("父分类不存在");
        }
        if (parent.getLevel() >= 3) {
            throw new BizException("三级分类下不能再添加子分类");
        }
    }

    private void validateNotDescendant(Long currentId, Long newParentId) {
        if (newParentId.equals(currentId)) {
            throw new BizException("不能将自己设为自己的父分类");
        }
        Long pid = newParentId;
        while (pid != null && pid != 0L) {
            Category p = getById(pid);
            if (p == null) {
                break;
            }
            if (p.getId().equals(currentId)) {
                throw new BizException("不能将分类移动到其子分类下");
            }
            pid = p.getParentId();
        }
    }

    @Override
    public Category getDefaultCategory() {
        Category def = baseMapper.selectOne(
                new LambdaQueryWrapper<Category>().eq(Category::getIsDefault, 1));
        if (def == null) {
            throw new BizException("默认分类不存在，请联系管理员");
        }
        return def;
    }

    @Override
    public List<Category> getShopTree() {
        List<Category> tree = getTree();
        return tree.stream()
                .filter(c -> !Integer.valueOf(1).equals(c.getIsDefault()))
                .collect(Collectors.toList());
    }



}
