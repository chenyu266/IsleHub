package com.islehub.product.mq;

import com.islehub.common.mq.OrderCreatedEvent;
import com.islehub.product.mapper.ProductSkuMapper;
import com.islehub.product.service.StockCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 订单创建后异步落库：将 Redis 预扣的库存同步到数据库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockConfirmConsumer {

    private final ProductSkuMapper skuMapper;
    private final StringRedisTemplate redisTemplate;
    private final StockCacheService stockCacheService;

    @RabbitListener(queues = "islehub.stock.confirm")
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        // ── 原子幂等校验：SETNX 认领消息 ──
        String idempotentKey = "stock:confirm:" + event.getOrderId();
        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", Duration.ofMinutes(10));
        if (Boolean.FALSE.equals(claimed)) {
            log.warn("库存确认已处理, orderId={}, 跳过重复消费", event.getOrderId());
            return;
        }

        try {
            log.info("收到订单创建事件 orderId={} items={}", event.getOrderId(), event.getItems().size());
            for (OrderCreatedEvent.OrderItemStock item : event.getItems()) {
                int rows = skuMapper.deductStock(item.getSkuId(), item.getQuantity());
                if (rows == 0) {
                    // 部分 SKU 扣减失败 → 抛异常回滚整个事务，删除幂等键允许 MQ 重试
                    redisTemplate.delete(idempotentKey);
                    throw new RuntimeException(String.format(
                            "库存落库失败 orderId=%s skuId=%s quantity=%s — DB 库存不足，事务回滚等待重试",
                            event.getOrderId(), item.getSkuId(), item.getQuantity()));
                }
                stockCacheService.invalidate(item.getSkuId());
            }
            log.info("库存落库完成 orderId={}", event.getOrderId());
        } catch (Exception e) {
            // 异常时确保幂等键已清除，允许 MQ 重试
            redisTemplate.delete(idempotentKey);
            throw e;
        }
    }
}
