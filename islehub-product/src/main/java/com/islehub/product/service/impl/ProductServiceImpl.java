package com.islehub.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.islehub.common.exception.BizException;
import com.islehub.common.redis.CacheService;
import com.islehub.common.redis.RedisKeys;
import com.islehub.product.entity.Category;
import com.islehub.product.entity.Product;
import com.islehub.product.entity.ProductSku;
import com.islehub.product.mapper.ProductMapper;
import com.islehub.product.mapper.ProductSkuMapper;
import com.islehub.product.service.CategoryService;
import com.islehub.product.service.ProductService;
import com.islehub.product.service.StockCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Value("${upload.path:uploads}")
    private String uploadPath;

    private final ProductSkuMapper skuMapper;
    private final CategoryService categoryService;
    private final StockCacheService stockCacheService;
    private final CacheService cacheService;

    public ProductServiceImpl(ProductSkuMapper skuMapper, CategoryService categoryService,
                              StockCacheService stockCacheService, CacheService cacheService) {
        this.skuMapper = skuMapper;
        this.categoryService = categoryService;
        this.stockCacheService = stockCacheService;
        this.cacheService = cacheService;
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
        String key = RedisKeys.productDetail(id);
        Product product = cacheService.getOrLoad(key, Product.class, RedisKeys.PRODUCT_DETAIL_TTL, () -> {
            return baseMapper.selectDetailById(id);
        });
        if (product == null) {
            throw new BizException("商品不存在");
        }
        return product;
    }

    @Override
    @Transactional
    public void addProduct(Product product, List<ProductSku> skus) {
        if (product.getCategoryId() == null || product.getCategoryId() <= 0) {
            product.setCategoryId(categoryService.getDefaultCategory().getId());
        }
        validateCategory(product.getCategoryId());
        save(product);
        skus.forEach(s -> s.setProductId(product.getId()));
        skus.forEach(skuMapper::insert);
    }

    @Override
    @Transactional
    public void updateProduct(Product product, List<ProductSku> skus) {
        if (product.getCategoryId() == null || product.getCategoryId() <= 0) {
            product.setCategoryId(categoryService.getDefaultCategory().getId());
        }
        validateCategory(product.getCategoryId());
        Product oldProduct = getById(product.getId());
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
                stockCacheService.invalidate(s.getId());
            } else {
                skuMapper.insert(s);
            }
        }

        // 清理被移除或替换的旧图片
        if (oldProduct != null) {
            deleteOrphanImages(oldProduct, product);
        }

        // 更新后失效详情缓存
        cacheService.evict(RedisKeys.productDetail(productId));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        updateById(product);

        cacheService.evict(RedisKeys.productDetail(id));
    }

    @Override
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) return;
        update(new LambdaUpdateWrapper<Product>()
                .set(Product::getStatus, status)
                .in(Product::getId, ids));

        String[] keys = ids.stream().map(RedisKeys::productDetail).toArray(String[]::new);
        cacheService.evict(keys);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = getById(id);
        if (product == null) {
            throw new BizException("商品不存在");
        }
        removeById(id);
        deleteImageFiles(product);

        // 删除后失效详情缓存
        cacheService.evict(RedisKeys.productDetail(id));
    }

    // ──────────────── 图片清理 ────────────────

    private void deleteImageFiles(Product product) {
        File baseDir = resolveUploadDir();
        deleteFile(baseDir, product.getMainImage());
        if (product.getImages() != null && !product.getImages().isBlank()) {
            Arrays.stream(product.getImages().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(url -> deleteFile(baseDir, url));
        }
    }

    private void deleteOrphanImages(Product oldProduct, Product newProduct) {
        File baseDir = resolveUploadDir();

        String oldMain = oldProduct.getMainImage();
        String newMain = newProduct.getMainImage();
        if (oldMain != null && !oldMain.isBlank() && !oldMain.equals(newMain)) {
            deleteFile(baseDir, oldMain);
        }

        Set<String> newImageSet = parseImageSet(newProduct.getImages());
        if (oldProduct.getImages() != null && !oldProduct.getImages().isBlank()) {
            Arrays.stream(oldProduct.getImages().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(url -> !newImageSet.contains(url))
                    .forEach(url -> deleteFile(baseDir, url));
        }
    }

    private Set<String> parseImageSet(String images) {
        if (images == null || images.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private void deleteFile(File baseDir, String url) {
        if (url == null || url.isBlank()) return;
        try {
            String filename = url.substring(url.lastIndexOf('/') + 1);
            Path filePath = Paths.get(baseDir.getAbsolutePath(), "products", filename);
            File file = filePath.toFile();
            if (file.exists() && file.isFile()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    log.warn("删除商品图片失败: {}", filePath);
                }
            }
        } catch (Exception e) {
            log.warn("删除商品图片异常 url={}: {}", url, e.getMessage());
        }
    }

    private File resolveUploadDir() {
        File dir = new File(uploadPath);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), uploadPath);
        }
        return dir;
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new BizException("请选择商品分类");
        }
        Category category = categoryService.getById(categoryId);
        if (category == null) {
            throw new BizException("商品分类不存在");
        }
        boolean isDefault = Integer.valueOf(1).equals(category.getIsDefault());
        if (!isDefault && category.getLevel() != 3) {
            throw new BizException("商品只能关联到三级分类");
        }
    }

    // ──────────────── 跨模块 SKU 操作 ────────────────

    @Override
    public ProductSku getSkuById(Long id) {
        return skuMapper.selectById(id);
    }

    @Override
    public void addSkuStock(Long skuId, int quantity) {
        skuMapper.addStock(skuId, quantity);
    }

    @Override
    public List<ProductSku> getSkusByProductIds(List<Long> productIds) {
        return skuMapper.selectByProductIds(productIds);
    }

    @Override
    public SkuDetail getSkuDetail(Long skuId) {
        return skuMapper.selectSkuDetail(skuId);
    }
}
