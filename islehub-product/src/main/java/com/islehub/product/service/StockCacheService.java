package com.islehub.product.service;

import com.islehub.common.exception.BizException;
import com.islehub.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 库存缓存 — 原子预扣 + 回滚。
 * <p>
 * 库存 Key: {@code stock:{skuId}} = 当前可售库存。
 * 首次访问时从 DB 惰性加载；数据库侧由 MQ 消费者异步落库。
 */
@Service
@RequiredArgsConstructor
public class StockCacheService {

    private static final String STOCK_PREFIX = "stock:";

    private final StringRedisTemplate redisTemplate;
    private final ProductSkuMapper skuMapper;

    /**
     * Lua 脚本：原子检查 + 扣减多个 SKU。
     * KEYS[]   = stock:{skuId}  for each SKU
     * ARGV[]   = quantity       for each SKU
     * Returns: 1 = 全部成功扣减, -1 = 库存不足
     */
    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT;

    static {
        DEDUCT_SCRIPT = new DefaultRedisScript<>();
        DEDUCT_SCRIPT.setResultType(Long.class);
        DEDUCT_SCRIPT.setScriptText(
            "for i=1,#KEYS do " +
            "  local cur = tonumber(redis.call('GET', KEYS[i]) or '0') " +
            "  if cur < tonumber(ARGV[i]) then " +
            "    return -1 " +
            "  end " +
            "end " +
            "for i=1,#KEYS do " +
            "  redis.call('DECRBY', KEYS[i], ARGV[i]) " +
            "end " +
            "return 1"
        );
    }

    // ────────────────────── public API ──────────────────────

    /** 原子预扣多 SKU 库存。成功返回 true，失败（库存不足）抛 BizException。 */
    public void tryDeduct(List<Long> skuIds, List<Integer> quantities) {
        if (skuIds.size() != quantities.size()) {
            throw new IllegalArgumentException("skuIds 和 quantities 长度不匹配");
        }

        // 惰性加载：确保所有 key 存在
        for (int i = 0; i < skuIds.size(); i++) {
            String key = stockKey(skuIds.get(i));
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                loadFromDb(skuIds.get(i));
            }
        }

        List<String> keys = skuIds.stream().map(this::stockKey).toList();
        List<String> args = quantities.stream().map(String::valueOf).toList();

        Long result = redisTemplate.execute(DEDUCT_SCRIPT, keys, args.toArray());
        if (result == null || result != 1) {
            throw new BizException("库存不足");
        }
    }

    /** 回滚预扣的库存（订单创建失败时调用）。 */
    public void rollback(List<Long> skuIds, List<Integer> quantities) {
        for (int i = 0; i < skuIds.size(); i++) {
            redisTemplate.opsForValue().increment(stockKey(skuIds.get(i)), quantities.get(i));
        }
    }

    // ────────────────────── internal ──────────────────────

    private String stockKey(Long skuId) {
        return STOCK_PREFIX + skuId;
    }

    private void loadFromDb(Long skuId) {
        var sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException("SKU 不存在: " + skuId);
        }
        redisTemplate.opsForValue().set(stockKey(skuId), String.valueOf(sku.getStock()));
    }
}
