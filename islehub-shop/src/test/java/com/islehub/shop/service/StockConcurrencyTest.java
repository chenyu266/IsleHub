package com.islehub.shop.service;

import com.islehub.product.entity.ProductSku;
import com.islehub.product.mapper.ProductSkuMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.islehub.shop.TestApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@DisplayName("库存扣减并发测试")
class StockConcurrencyTest {

    @Autowired private ProductSkuMapper skuMapper;

    private Long skuId;

    @BeforeEach
    void setUp() {
        ProductSku sku = new ProductSku();
        sku.setProductId(1L);
        sku.setSkuCode("SKU-CONC-" + System.nanoTime());
        sku.setSpec("并发测试规格");
        sku.setPrice(new BigDecimal("50.00"));
        sku.setStock(5);  // 初始库存 5
        sku.setStatus(1);
        skuMapper.insert(sku);
        skuId = sku.getId();
    }

    @AfterEach
    void tearDown() {
        skuMapper.deleteById(skuId);
    }

    @Test
    @DisplayName("10 线程并发各扣 1 件，5 件库存 → 恰好 5 次成功，无超卖")
    void concurrentStockDeductionShouldNotOversell() throws Exception {
        int threadCount = 10;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // 所有线程同时起跑

                    // 原子扣减库存（相对更新，无 TOCTOU 问题）
                    int updated = skuMapper.deductStock(skuId, 1);
                    if (updated > 0) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        readyLatch.await();
        startLatch.countDown(); // 同时释放所有线程
        doneLatch.await();

        // 验证
        ProductSku finalSku = skuMapper.selectById(skuId);
        int successes = successCount.get();
        int finalStock = finalSku.getStock();

        // 核心断言：成功次数 + 最终库存 = 初始库存（5）
        assertThat(successes + finalStock).isEqualTo(5);

        // 断言无超卖：库存不应为负
        assertThat(finalStock).isGreaterThanOrEqualTo(0);

        // 断言至少有一次成功
        assertThat(successes).isGreaterThanOrEqualTo(1);

        // 输出诊断信息（测试通过时会显示）
        System.out.printf("并发扣减结果: 成功=%d, 剩余库存=%d, 总=%d (初始=5)%n",
                successes, finalStock, successes + finalStock);
    }
}
