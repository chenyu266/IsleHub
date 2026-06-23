package com.islehub.shop.service;

import com.islehub.order.entity.Order;
import com.islehub.order.entity.OrderItem;
import com.islehub.order.entity.OrderShipping;
import com.islehub.order.mapper.OrderItemMapper;
import com.islehub.order.mapper.OrderMapper;
import com.islehub.order.mapper.OrderShippingMapper;
import com.islehub.product.entity.ProductSku;
import com.islehub.product.mapper.ProductSkuMapper;
import com.islehub.shop.TestApplication;
import com.islehub.shop.TestRedisConfig;
import com.islehub.shop.entity.Address;
import com.islehub.shop.mapper.AddressMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {TestApplication.class, TestRedisConfig.class})
@ActiveProfiles("test")
@Transactional
@DisplayName("订单流程集成测试")
class OrderFlowIntegrationTest {

    @Autowired private ShopOrderService shopOrderService;
    @Autowired private ProductSkuMapper skuMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderItemMapper itemMapper;
    @Autowired private OrderShippingMapper shippingMapper;
    @Autowired private AddressMapper addressMapper;

    @MockBean private CartService cartService;

    private Long userId = 1L;
    private Long skuId;
    private Long addressId;

    @BeforeEach
    void setUp() {
        ProductSku sku = new ProductSku();
        sku.setProductId(1L);
        sku.setSkuCode("SKU-INT-" + System.nanoTime());
        sku.setSpec("红色/XL");
        sku.setPrice(new BigDecimal("99.00"));
        sku.setStock(10);
        sku.setStatus(1);
        skuMapper.insert(sku);
        skuId = sku.getId();

        Address addr = new Address();
        addr.setUserId(userId);
        addr.setReceiverName("李四");
        addr.setReceiverPhone("13900139000");
        addr.setProvince("北京");
        addr.setCity("北京");
        addr.setDistrict("朝阳区");
        addr.setDetail("望京路1号");
        addr.setIsDefault(1);
        addressMapper.insert(addr);
        addressId = addr.getId();
    }

    @Test
    @DisplayName("下单 → 扣库存 → 创建订单和订单项")
    void checkoutShouldCreateOrderAndDeductStock() {
        CartService.CartItem cartItem = new CartService.CartItem();
        cartItem.setSkuId(skuId);
        cartItem.setProductId(1L);
        cartItem.setProductName("测试商品");
        cartItem.setSkuSpec("红色/XL");
        cartItem.setPrice(new BigDecimal("99.00"));
        cartItem.setQuantity(3);
        when(cartService.list(userId)).thenReturn(List.of(cartItem));

        ShopOrderService.CheckoutResult result = shopOrderService.checkout(userId, addressId, "请尽快发货");

        // 订单已创建
        Order order = result.getOrder();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo("paid");
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("297.00"));
        assertThat(order.getReceiverName()).isEqualTo("李四");

        // 库存已扣减
        ProductSku sku = skuMapper.selectById(skuId);
        assertThat(sku.getStock()).isEqualTo(7);

        // 订单项已创建
        List<OrderItem> items = itemMapper.selectList(null);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getQuantity()).isEqualTo(3);

        // 购物车已清空
        verify(cartService).clear(userId);
    }

    @Test
    @DisplayName("取消订单 → 状态变 cancelled → 库存恢复")
    void cancelOrderShouldRestoreStockAndUpdateStatus() {
        // 准备：手动创建订单（跳过 checkout 流程）
        Order order = new Order();
        order.setOrderNo("ORD-CANCEL-001");
        order.setUserId(userId);
        order.setStatus("paid");
        order.setTotalAmount(new BigDecimal("198.00"));
        order.setReceiverName("测试");
        order.setReceiverPhone("13800000000");
        order.setReceiverAddress("测试地址");
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(1L);
        item.setSkuId(skuId);
        item.setProductName("测试商品");
        item.setSkuSpec("红色/XL");
        item.setPrice(new BigDecimal("99.00"));
        item.setQuantity(2);
        itemMapper.insert(item);

        // 手动扣减库存（模拟下单后的状态）
        ProductSku sku = skuMapper.selectById(skuId);
        sku.setStock(8);
        skuMapper.updateById(sku);

        // 执行取消
        shopOrderService.cancelOrder(order.getId(), userId);

        // 验证
        Order cancelled = orderMapper.selectById(order.getId());
        assertThat(cancelled.getStatus()).isEqualTo("cancelled");

        ProductSku restored = skuMapper.selectById(skuId);
        assertThat(restored.getStock()).isEqualTo(10); // 恢复 2
    }

    @Test
    @DisplayName("确认收货 → 状态变 completed → 更新发货时间")
    void confirmOrderShouldCompleteAndSetDeliveredAt() {
        Order order = new Order();
        order.setOrderNo("ORD-CONFIRM-001");
        order.setUserId(userId);
        order.setStatus("shipped");
        order.setTotalAmount(new BigDecimal("99.00"));
        order.setReceiverName("测试");
        order.setReceiverPhone("13800000000");
        order.setReceiverAddress("测试地址");
        orderMapper.insert(order);

        OrderShipping shipping = new OrderShipping();
        shipping.setOrderId(order.getId());
        shipping.setCarrier("顺丰");
        shipping.setTrackingNo("SF123456");
        shipping.setShippedAt(LocalDateTime.now());
        shippingMapper.insert(shipping);

        shopOrderService.confirmOrder(order.getId(), userId);

        Order completed = orderMapper.selectById(order.getId());
        assertThat(completed.getStatus()).isEqualTo("completed");

        OrderShipping updated = shippingMapper.selectById(shipping.getId());
        assertThat(updated.getDeliveredAt()).isNotNull();
    }

    @Test
    @DisplayName("库存不足时下单 → 抛异常，无订单创建")
    void shouldFailWhenStockInsufficient() {
        CartService.CartItem cartItem = new CartService.CartItem();
        cartItem.setSkuId(skuId);
        cartItem.setProductId(1L);
        cartItem.setProductName("测试商品");
        cartItem.setSkuSpec("红色/XL");
        cartItem.setPrice(new BigDecimal("99.00"));
        cartItem.setQuantity(100); // 超过库存 10
        when(cartService.list(userId)).thenReturn(List.of(cartItem));

        assertThatThrownBy(() -> shopOrderService.checkout(userId, addressId, null))
                .hasMessageContaining("库存不足");

        assertThat(orderMapper.selectList(null)).isEmpty();
    }

    @Test
    @DisplayName("价格变动 → 返回 warnings")
    void shouldWarnOnPriceChange() {
        CartService.CartItem cartItem = new CartService.CartItem();
        cartItem.setSkuId(skuId);
        cartItem.setProductId(1L);
        cartItem.setProductName("测试商品");
        cartItem.setSkuSpec("红色/XL");
        cartItem.setPrice(new BigDecimal("88.00")); // 加购价 88
        cartItem.setQuantity(1);
        when(cartService.list(userId)).thenReturn(List.of(cartItem));

        ShopOrderService.CheckoutResult result = shopOrderService.checkout(userId, addressId, null);

        assertThat(result.getWarnings()).isNotEmpty();
        assertThat(result.getWarnings().get(0)).contains("价格已变动");
    }
}
