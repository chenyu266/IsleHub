package com.islehub.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.islehub.order.entity.OrderMessage;
import com.islehub.order.mapper.OrderMessageMapper;
import com.islehub.order.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageRetryTask {

    private final OrderMessageMapper orderMessageMapper;
    private final CheckoutService checkoutService;

    /**
     * 每 1 分钟扫描一次，重试发送失败的消息
     */
    @Scheduled(fixedDelay = 60000)
    public void retryFailedMessages() {
        List<OrderMessage> messages = orderMessageMapper.selectList(
                new LambdaQueryWrapper<OrderMessage>()
                        .and(w -> w
                                .eq(OrderMessage::getStatus, 0)   // 待发送
                                .or()
                                .eq(OrderMessage::getStatus, 2)   // 发送失败但未超重试上限
                        )
                        .lt(OrderMessage::getRetryCount, 3)
                        .lt(OrderMessage::getCreatedAt, LocalDateTime.now().minusSeconds(10))
        );

        for (OrderMessage msg : messages) {
            log.info("重试发送订单消息, orderId={}, retryCount={}", msg.getOrderId(), msg.getRetryCount());
            checkoutService.sendOrderMessage(msg.getOrderId());
        }
    }
}
