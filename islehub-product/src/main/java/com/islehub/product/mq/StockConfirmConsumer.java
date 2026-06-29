package com.islehub.product.mq;

import com.islehub.common.mq.OrderCreatedEvent;
import com.islehub.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单创建后异步落库：将 Redis 预扣的库存同步到数据库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockConfirmConsumer {

    private final ProductSkuMapper skuMapper;

    @RabbitListener(queues = "islehub.stock.confirm")
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("收到订单创建事件 orderId={} items={}", event.getOrderId(), event.getItems().size());
        for (OrderCreatedEvent.OrderItemStock item : event.getItems()) {
            int rows = skuMapper.deductStock(item.getSkuId(), item.getQuantity());
            if (rows == 0) {
                log.error("库存落库失败 orderId={} skuId={} quantity={} — DB 库存不足！",
                        event.getOrderId(), item.getSkuId(), item.getQuantity());
            }
        }
        log.info("库存落库完成 orderId={}", event.getOrderId());
    }
}
