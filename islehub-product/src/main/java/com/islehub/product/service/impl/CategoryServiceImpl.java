package com.islehub.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islehub.product.entity.Category;
import com.islehub.product.mapper.CategoryMapper;
import com.islehub.product.service.CategoryService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private static final String CACHE_KEY = "category:tree";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
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
        List<Category> all = list();
        Map<Long, List<Category>> parentMap = all.stream()
                .collect(Collectors.groupingBy(Category::getParentId));
        List<Category> roots = parentMap.getOrDefault(0L, List.of());
        roots.forEach(root -> buildChildren(root, parentMap));

        // 3. 写缓存
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(roots), CACHE_TTL);
        } catch (JsonProcessingException ignored) {
        }

        return roots;
    }

    private void buildChildren(Category parent, Map<Long, List<Category>> parentMap) {
        List<Category> children = parentMap.getOrDefault(parent.getId(), List.of());
        parent.setChildren(children);
        children.forEach(c -> buildChildren(c, parentMap));
    }

    // ---------- 增删改时清缓存 ----------

    @Override
    public boolean save(Category entity) {
        boolean result = super.save(entity);
        redisTemplate.delete(CACHE_KEY);
        return result;
    }

    @Override
    public boolean updateById(Category entity) {
        boolean result = super.updateById(entity);
        redisTemplate.delete(CACHE_KEY);
        return result;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean result = super.removeById(id);
        redisTemplate.delete(CACHE_KEY);
        return result;
    }
}
