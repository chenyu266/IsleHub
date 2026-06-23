package com.islehub.shop.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.islehub.common.exception.BizException;
import com.islehub.order.entity.Order;
import com.islehub.order.entity.OrderItem;
import com.islehub.order.mapper.OrderItemMapper;
import com.islehub.order.mapper.OrderMapper;
import com.islehub.order.mapper.OrderShippingMapper;
import com.islehub.product.entity.ProductSku;
import com.islehub.product.mapper.ProductSkuMapper;
import com.islehub.shop.entity.Address;
import com.islehub.shop.mapper.AddressMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopOrderService 单元测试")
class ShopOrderServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, ProductSku.class);
        TableInfoHelper.initTableInfo(assistant, OrderItem.class);
        TableInfoHelper.initTableInfo(assistant, Order.class);
    }

    @Mock OrderMapper orderMapper;
    @Mock OrderItemMapper itemMapper;
    @Mock OrderShippingMapper shippingMapper;
    @Mock CartService cartService;
    @Mock ProductSkuMapper skuMapper;
    @Mock AddressMapper addressMapper;

    private ShopOrderService service;

    // ── 测试数据 ──
    private final Long userId = 1L;
    private final Long addressId = 10L;
    private final Long skuId = 100L;
    private final Long orderId = 1000L;

    private Address address;
    private ProductSku sku;
    private CartService.CartItem cartItem;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new ShopOrderService(itemMapper, shippingMapper, cartService, skuMapper, addressMapper);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);

        address = new Address();
        address.setId(addressId);
        address.setUserId(userId);
        address.setReceiverName("张三");
        address.setReceiverPhone("13800138000");
        address.setProvince("广东");
        address.setCity("深圳");
        address.setDistrict("南山区");
        address.setDetail("科技园路1号");

        sku = new ProductSku();
        sku.setId(skuId);
        sku.setProductId(200L);
        sku.setStock(10);
        sku.setPrice(new BigDecimal("99.00"));
        sku.setStatus(1);

        cartItem = new CartService.CartItem();
        cartItem.setSkuId(skuId);
        cartItem.setProductId(200L);
        cartItem.setProductName("测试商品");
        cartItem.setSkuSpec("红色/XL");
        cartItem.setPrice(new BigDecimal("99.00"));
        cartItem.setQuantity(2);

        order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setOrderNo("ORDER001");
        order.setStatus("paid");
    }

    // ═══════════════════════════════════════════
    // checkout 测试
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("checkout")
    class CheckoutTests {

        @Test
        @DisplayName("正常下单 → 扣减库存、创建订单和订单项、清空购物车")
        void shouldCreateOrderAndDeductStock() {
            when(cartService.list(userId)).thenReturn(List.of(cartItem));
            when(addressMapper.selectById(addressId)).thenReturn(address);
            when(skuMapper.selectById(skuId)).thenReturn(sku);
            when(skuMapper.deductStock(eq(skuId), anyInt())).thenReturn(1);
            when(orderMapper.insert(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(orderId);
                return 1;
            });
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);

            ShopOrderService.CheckoutResult result = service.checkout(userId, addressId, null);

            assertThat(result.getOrder()).isNotNull();
            assertThat(result.getOrder().getStatus()).isEqualTo("paid");
            assertThat(result.getOrder().getUserId()).isEqualTo(userId);
            assertThat(result.getOrder().getTotalAmount()).isEqualByComparingTo(new BigDecimal("198.00"));
            assertThat(result.getWarnings()).isEmpty();

            // 验证购物车已清空
            verify(cartService).clear(userId);
            // 验证订单项已插入
            ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
            verify(itemMapper).insert(itemCaptor.capture());
            assertThat(itemCaptor.getValue().getQuantity()).isEqualTo(2);
            assertThat(itemCaptor.getValue().getSkuId()).isEqualTo(skuId);
        }

        @Test
        @DisplayName("购物车为空 → 抛 BizException")
        void shouldThrowWhenCartEmpty() {
            when(cartService.list(userId)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.checkout(userId, addressId, null))
                    .isInstanceOf(BizException.class)
                    .hasMessage("购物车为空");
        }

        @Test
        @DisplayName("收货地址不存在或不属于该用户 → 抛 BizException")
        void shouldThrowWhenAddressInvalid() {
            when(cartService.list(userId)).thenReturn(List.of(cartItem));
            when(addressMapper.selectById(addressId)).thenReturn(null);

            assertThatThrownBy(() -> service.checkout(userId, addressId, null))
                    .isInstanceOf(BizException.class)
                    .hasMessage("收货地址不存在");
        }

        @Test
        @DisplayName("SKU 不存在 → 抛 BizException")
        void shouldThrowWhenSkuNotFound() {
            when(cartService.list(userId)).thenReturn(List.of(cartItem));
            when(addressMapper.selectById(addressId)).thenReturn(address);
            when(skuMapper.selectById(skuId)).thenReturn(null);

            assertThatThrownBy(() -> service.checkout(userId, addressId, null))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("不存在");
        }

        @Test
        @DisplayName("库存不足 → 抛 BizException（update 返回 0）")
        void shouldThrowWhenStockInsufficient() {
            when(cartService.list(userId)).thenReturn(List.of(cartItem));
            when(addressMapper.selectById(addressId)).thenReturn(address);
            when(skuMapper.selectById(skuId)).thenReturn(sku);
            when(skuMapper.deductStock(eq(skuId), anyInt())).thenReturn(0);

            assertThatThrownBy(() -> service.checkout(userId, addressId, null))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("库存不足");
        }

        @Test
        @DisplayName("价格变动 → 返回 warnings")
        void shouldWarnWhenPriceChanged() {
            CartService.CartItem staleItem = new CartService.CartItem();
            staleItem.setSkuId(skuId);
            staleItem.setProductId(200L);
            staleItem.setProductName("测试商品");
            staleItem.setSkuSpec("红色/XL");
            staleItem.setPrice(new BigDecimal("88.00")); // 加购时价格
            staleItem.setQuantity(2);

            when(cartService.list(userId)).thenReturn(List.of(staleItem));
            when(addressMapper.selectById(addressId)).thenReturn(address);
            when(skuMapper.selectById(skuId)).thenReturn(sku); // 现价 99.00
            when(skuMapper.deductStock(eq(skuId), anyInt())).thenReturn(1);
            when(orderMapper.insert(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(orderId);
                return 1;
            });
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);

            ShopOrderService.CheckoutResult result = service.checkout(userId, addressId, null);

            assertThat(result.getWarnings()).isNotEmpty();
            assertThat(result.getWarnings().get(0)).contains("价格已变动");
        }
    }

    // ═══════════════════════════════════════════
    // cancelOrder 测试
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("正常取消 → 状态变 cancelled、恢复库存")
        void shouldCancelAndRestoreStock() {
            Order paidOrder = new Order();
            paidOrder.setId(orderId);
            paidOrder.setUserId(userId);
            paidOrder.setStatus("paid");

            OrderItem item = new OrderItem();
            item.setSkuId(skuId);
            item.setQuantity(2);

            when(orderMapper.selectById(orderId)).thenReturn(paidOrder);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);
            when(itemMapper.selectList(any())).thenReturn(List.of(item));
            when(skuMapper.addStock(eq(skuId), anyInt())).thenReturn(1);

            service.cancelOrder(orderId, userId);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderMapper).updateById(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo("cancelled");
        }

        @Test
        @DisplayName("订单不属于该用户 → 抛 BizException")
        void shouldThrowWhenOrderNotBelongToUser() {
            Order paidOrder = new Order();
            paidOrder.setId(orderId);
            paidOrder.setUserId(999L); // 另一个用户
            paidOrder.setStatus("paid");

            when(orderMapper.selectById(orderId)).thenReturn(paidOrder);

            assertThatThrownBy(() -> service.cancelOrder(orderId, userId))
                    .isInstanceOf(BizException.class)
                    .hasMessage("订单不存在");
        }

        @Test
        @DisplayName("非 paid 状态 → 抛 BizException")
        void shouldThrowWhenStatusNotPaid() {
            Order shippedOrder = new Order();
            shippedOrder.setId(orderId);
            shippedOrder.setUserId(userId);
            shippedOrder.setStatus("shipped");

            when(orderMapper.selectById(orderId)).thenReturn(shippedOrder);

            assertThatThrownBy(() -> service.cancelOrder(orderId, userId))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("只能取消已支付");
        }
    }

    // ═══════════════════════════════════════════
    // confirmOrder 测试
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("confirmOrder")
    class ConfirmOrderTests {

        @Test
        @DisplayName("正常确认 → 状态变 completed")
        void shouldConfirmOrder() {
            Order shippedOrder = new Order();
            shippedOrder.setId(orderId);
            shippedOrder.setUserId(userId);
            shippedOrder.setStatus("shipped");

            when(orderMapper.selectById(orderId)).thenReturn(shippedOrder);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);
            when(shippingMapper.selectOne(any())).thenReturn(null);

            service.confirmOrder(orderId, userId);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderMapper).updateById(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo("completed");
        }

        @Test
        @DisplayName("非 shipped 状态 → 抛 BizException")
        void shouldThrowWhenStatusNotShipped() {
            Order paidOrder = new Order();
            paidOrder.setId(orderId);
            paidOrder.setUserId(userId);
            paidOrder.setStatus("paid");

            when(orderMapper.selectById(orderId)).thenReturn(paidOrder);

            assertThatThrownBy(() -> service.confirmOrder(orderId, userId))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("只能确认已发货");
        }
    }
}
