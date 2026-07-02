package com.islehub.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islehub.common.exception.BizException;
import com.islehub.product.service.ProductService;
import com.islehub.product.service.ProductService.SkuDetail;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 购物车业务服务 —— 基于 Redis 的购物车管理，使用 CAS 机制保证并发安全。
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final StringRedisTemplate redisTemplate;
    private final ProductService productService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * CAS 写入：比较 version，匹配则覆盖；不匹配返回 0。
     * KEYS[1]=cart key, ARGV[1]=field, ARGV[2]=new JSON, ARGV[3]=expected version, ARGV[4]=new version.
     * Version 存储在独立 hash field {@code field:ver}，不嵌入 JSON。
     */
    private static final DefaultRedisScript<Long> CAS_SET;

    static {
        CAS_SET = new DefaultRedisScript<>();
        CAS_SET.setResultType(Long.class);
        CAS_SET.setScriptText(
            "local ver_field = ARGV[1] .. ':ver'; " +
            "local current_ver = redis.call('HGET', KEYS[1], ver_field); " +
            "local expected = tonumber(ARGV[3]); " +
            "if not current_ver then " +
            "  if expected == -1 then " +
            "    redis.call('HSET', KEYS[1], ARGV[1], ARGV[2]); " +
            "    redis.call('HSET', KEYS[1], ver_field, ARGV[4]); " +
            "    redis.call('EXPIRE', KEYS[1], ARGV[5]); " +
            "    return 1; " +
            "  end; " +
            "  return 0; " +
            "end; " +
            "if tonumber(current_ver) == expected then " +
            "  redis.call('HSET', KEYS[1], ARGV[1], ARGV[2]); " +
            "  redis.call('HSET', KEYS[1], ver_field, ARGV[4]); " +
            "  redis.call('EXPIRE', KEYS[1], ARGV[5]); " +
            "  return 1; " +
            "end; " +
            "return 0;");
    }

    private static final java.time.Duration CART_TTL = java.time.Duration.ofDays(7);

    private String key(Long userId) {
        return "IsleHub:cart:" + userId;
    }

    public List<CartItem> list(Long userId) {
        String cartKey = key(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey);
        List<CartItem> items = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String field = (String) entry.getKey();
            if (field.endsWith(":ver")) continue;
            try {
                CartItem item = objectMapper.readValue((String) entry.getValue(), CartItem.class);
                String verStr = (String) redisTemplate.opsForHash().get(cartKey, field + ":ver");
                item.setVersion(verStr != null ? Long.parseLong(verStr) : 0L);
                items.add(item);
            } catch (JsonProcessingException e) {
                throw new BizException("购物车数据异常");
            }
        }
        return items;
    }

    public void add(Long userId, Long skuId, Integer quantity) {
        SkuDetail detail = productService.getSkuDetail(skuId);
        if (detail == null || detail.getStatus() == null || detail.getStatus() == 0
                || detail.getStock() == null || detail.getStock() <= 0) {
            throw new BizException("SKU 不存在、已下架或库存不足");
        }
        String cartKey = key(userId);
        String field = skuId.toString();
        for (int retry = 0; retry < 3; retry++) {
            CartItem current = getCartItem(cartKey, field);
            CartItem next;
            long expectedVersion;
            if (current == null) {
                next = new CartItem();
                next.setSkuId(skuId);
                next.setProductId(detail.getProductId());
                next.setProductName(detail.getProductName() != null ? detail.getProductName() : "");
                next.setProductImage(detail.getProductImage() != null ? detail.getProductImage() : null);
                next.setSkuSpec(detail.getSpec());
                next.setPrice(detail.getPrice());
                next.setQuantity(quantity);
                next.setVersion(0L);
                expectedVersion = -1L;
            } else {
                next = new CartItem();
                next.setSkuId(current.getSkuId());
                next.setProductId(current.getProductId());
                next.setProductName(detail.getProductName() != null ? detail.getProductName() : current.getProductName());
                next.setProductImage(detail.getProductImage() != null ? detail.getProductImage() : current.getProductImage());
                next.setSkuSpec(detail.getSpec() != null ? detail.getSpec() : current.getSkuSpec());
                next.setPrice(detail.getPrice());
                next.setQuantity(current.getQuantity() + quantity);
                next.setVersion(current.getVersion() + 1);
                expectedVersion = current.getVersion();
            }
            try {
                String json = objectMapper.writeValueAsString(next);
                Long result = redisTemplate.execute(CAS_SET,
                        Collections.singletonList(cartKey),
                        field, json, String.valueOf(expectedVersion),
                        String.valueOf(next.getVersion()),
                        String.valueOf(CART_TTL.getSeconds()));
                if (result != null && result == 1) return;
                Thread.sleep((long) (Math.random() * 20) + retry * 5L);
            } catch (JsonProcessingException e) {
                throw new BizException("购物车操作失败", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BizException("操作被中断");
            }
        }
        throw new BizException("操作冲突，请重试");
    }

    public void updateQuantity(Long userId, Long skuId, Integer quantity) {
        String cartKey = key(userId);
        String field = skuId.toString();
        for (int retry = 0; retry < 3; retry++) {
            CartItem current = getCartItem(cartKey, field);
            if (current == null) {
                throw new BizException("购物车中没有该商品");
            }
            CartItem next = new CartItem();
            next.setSkuId(current.getSkuId());
            next.setProductId(current.getProductId());
            next.setProductName(current.getProductName());
            next.setProductImage(current.getProductImage());
            next.setSkuSpec(current.getSkuSpec());
            next.setPrice(current.getPrice());
            next.setQuantity(quantity);
            next.setVersion(current.getVersion() + 1);
            try {
                String json = objectMapper.writeValueAsString(next);
                Long result = redisTemplate.execute(CAS_SET,
                        Collections.singletonList(cartKey),
                        field, json, String.valueOf(current.getVersion()),
                        String.valueOf(next.getVersion()),
                        String.valueOf(CART_TTL.getSeconds()));
                if (result != null && result == 1) return;
                Thread.sleep((long) (Math.random() * 20) + retry * 5L);
            } catch (JsonProcessingException e) {
                throw new BizException("购物车操作失败", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BizException("操作被中断");
            }
        }
        throw new BizException("操作冲突，请重试");
    }

    private CartItem getCartItem(String cartKey, String field) {
        String json = (String) redisTemplate.opsForHash().get(cartKey, field);
        if (json == null) return null;
        try {
            CartItem item = objectMapper.readValue(json, CartItem.class);
            String verStr = (String) redisTemplate.opsForHash().get(cartKey, field + ":ver");
            item.setVersion(verStr != null ? Long.parseLong(verStr) : 0L);
            return item;
        } catch (JsonProcessingException e) {
            throw new BizException("购物车数据异常");
        }
    }

    public void remove(Long userId, Long skuId) {
        String cartKey = key(userId);
        String field = skuId.toString();
        redisTemplate.opsForHash().delete(cartKey, field, field + ":ver");
    }

    public void clear(Long userId) {
        redisTemplate.delete(key(userId));
    }

    @Data
    public static class CartItem {
        private Long skuId;
        private Long productId;
        private String productName;
        private String productImage;
        private String skuSpec;
        private BigDecimal price;
        private Integer quantity;
        @com.fasterxml.jackson.annotation.JsonIgnore
        private Long version;
    }
}
