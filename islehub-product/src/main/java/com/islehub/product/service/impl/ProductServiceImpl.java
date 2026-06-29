package com.islehub.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.islehub.common.exception.BizException;
import com.islehub.product.entity.Product;
import com.islehub.product.entity.ProductSku;
import com.islehub.product.mapper.ProductMapper;
import com.islehub.product.mapper.ProductSkuMapper;
import com.islehub.product.service.CategoryService;
import com.islehub.product.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductSkuMapper skuMapper;
    private final CategoryService categoryService;

    public ProductServiceImpl(ProductSkuMapper skuMapper, CategoryService categoryService) {
        this.skuMapper = skuMapper;
        this.categoryService = categoryService;
    }


    @Override
    public Page<Product> pageProducts(int page, int pageSize, String keyword, Long categoryId, Integer status) {
        List<Long> categoryIds = categoryService.getCategoryAndChildrenIds(categoryId);


        Page<Product> result;
        if (categoryIds.isEmpty()) {
            result = baseMapper.pageProducts(new Page<>(page, pageSize), keyword, null, status);
        } else {
            result = baseMapper.pageProductsByCategoryIds(
                    new Page<>(page, pageSize), keyword, categoryIds, status);
        }

        if (!result.getRecords().isEmpty()) {
            List<Long> productIds = result.getRecords().stream().map(Product::getId).toList();
            List<ProductSku> allSkus = skuMapper.selectByProductIds(productIds);
            Map<Long, List<ProductSku>> skuMap = allSkus.stream()
                    .collect(Collectors.groupingBy(ProductSku::getProductId));
            result.getRecords().forEach(p ->
                    p.setSkus(skuMap.getOrDefault(p.getId(), List.of())));
        }
        return result;
    }

    @Override
    public Product getDetail(Long id) {
        Product product = baseMapper.selectDetailById(id);
        if (product == null) {
            throw new BizException("商品不存在");
        }
        return product;
    }

    @Override
    @Transactional
    public void addProduct(Product product, List<ProductSku> skus) {
        save(product);
        skus.forEach(s -> s.setProductId(product.getId()));
        skus.forEach(skuMapper::insert);
    }

    @Override
    @Transactional
    public void updateProduct(Product product, List<ProductSku> skus) {
        updateById(product);
        Long productId = product.getId();
        Set<Long> keepIds = skus.stream()
                .map(ProductSku::getId).filter(id -> id != null).collect(Collectors.toSet());
        if (!keepIds.isEmpty()) {
            skuMapper.delete(new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getProductId, productId)
                    .notIn(ProductSku::getId, keepIds));
        } else {
            skuMapper.delete(new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getProductId, productId));
        }
        for (ProductSku s : skus) {
            s.setProductId(productId);
            if (s.getId() != null) {
                skuMapper.updateById(s);
            } else {
                skuMapper.insert(s);
            }
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        updateById(product);
    }

    @Override
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) return;
        update(new LambdaUpdateWrapper<Product>()
                .set(Product::getStatus, status)
                .in(Product::getId, ids));
    }
}
