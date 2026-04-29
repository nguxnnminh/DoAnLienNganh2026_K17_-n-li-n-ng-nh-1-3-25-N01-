package com.shop.clothingstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shop.clothingstore.dto.CartItemDTO;
import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private CouponService couponService;

    @InjectMocks
    private CheckoutService checkoutService;

    private ProductVariant variant;
    private CartItemDTO cartItem;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setName("Test Shirt");
        product.setActive(true);

        variant = new ProductVariant();
        variant.setPrice(new BigDecimal("250000"));
        variant.setStock(10);
        variant.setSold(0);
        variant.setSize("M");
        variant.setColor("Black");
        variant.setProduct(product);

        cartItem = new CartItemDTO();
        cartItem.setVariantId(1L);
        cartItem.setProductName("Test Shirt");
        cartItem.setSize("M");
        cartItem.setColor("Black");
        cartItem.setPrice(new BigDecimal("250000"));
        cartItem.setQuantity(2);
    }

    // =====================================================
    // HAPPY PATH
    // =====================================================
    @Test
    void checkout_happyPath_createsOrderAndDeductsStock() {
        when(variantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(variant));
        when(couponService.applyCoupon(null, new BigDecimal("500000")))
                .thenReturn(new BigDecimal("500000"));

        Order saved = new Order();
        saved.setStatus(OrderStatus.PENDING);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        Order result = checkoutService.checkout("Nguyen Van A", "0901234567",
                "123 Le Loi", new ArrayList<>(List.of(cartItem)), null, null, null);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(variant.getStock()).isEqualTo(8);
        assertThat(variant.getSold()).isEqualTo(2);
        verify(variantRepository).save(variant);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void checkout_withCoupon_appliesDiscount() {
        when(variantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(variant));
        when(couponService.applyCoupon("SAVE10", new BigDecimal("500000")))
                .thenReturn(new BigDecimal("450000"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            assertThat(o.getTotal()).isEqualByComparingTo("450000");
            return o;
        });

        checkoutService.checkout("Nguyen Van A", "0901234567",
                "123 Le Loi", List.of(cartItem), null, "SAVE10", null);

        verify(couponService).applyCoupon("SAVE10", new BigDecimal("500000"));
    }

    @Test
    void checkout_withNote_savedOnOrder() {
        when(variantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(variant));
        when(couponService.applyCoupon(any(), any())).thenAnswer(inv -> inv.getArgument(1));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            assertThat(o.getNote()).isEqualTo("Please gift wrap");
            return o;
        });

        checkoutService.checkout("Nguyen Van A", "0901234567",
                "123 Le Loi", List.of(cartItem), null, null, "Please gift wrap");
    }

    // =====================================================
    // STOCK VALIDATION
    // =====================================================
    @Test
    void checkout_insufficientStock_throwsIllegalState() {
        variant.setStock(1);
        when(variantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> checkoutService.checkout("A", "0901234567", "addr",
                List.of(cartItem), null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("du so luong");

        assertThat(variant.getStock()).isEqualTo(1);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_emptyCart_throwsIllegalState() {
        assertThatThrownBy(() -> checkoutService.checkout("A", "0901234567", "addr",
                List.of(), null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trong");
    }

    @Test
    void checkout_nullCart_throwsIllegalState() {
        assertThatThrownBy(() -> checkoutService.checkout("A", "0901234567", "addr",
                null, null, null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkout_variantNotFound_throwsIllegalState() {
        when(variantRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.checkout("A", "0901234567", "addr",
                List.of(cartItem), null, null, null))
                .isInstanceOf(IllegalStateException.class);

        verify(orderRepository, never()).save(any());
    }

    // =====================================================
    // PRICE SNAPSHOT
    // =====================================================
    @Test
    void checkout_usesDatabasePriceNotCartPrice() {
        cartItem.setPrice(new BigDecimal("200000"));
        variant.setPrice(new BigDecimal("250000"));

        when(variantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(variant));
        when(couponService.applyCoupon(any(), any())).thenAnswer(inv -> inv.getArgument(1));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            assertThat(o.getTotal()).isEqualByComparingTo("500000");
            return o;
        });

        checkoutService.checkout("A", "0901234567", "addr",
                List.of(cartItem), null, null, null);
    }

    // =====================================================
    // MULTIPLE ITEMS
    // =====================================================
    @Test
    void checkout_multipleItems_allStockDeducted() {
        Product product2 = new Product();
        product2.setName("Test Pants");
        product2.setActive(true);

        ProductVariant variant2 = new ProductVariant();
        variant2.setPrice(new BigDecimal("350000"));
        variant2.setStock(5);
        variant2.setSold(0);
        variant2.setSize("L");
        variant2.setColor("Blue");
        variant2.setProduct(product2);

        CartItemDTO item2 = new CartItemDTO();
        item2.setVariantId(2L);
        item2.setProductName("Test Pants");
        item2.setSize("L");
        item2.setColor("Blue");
        item2.setPrice(new BigDecimal("350000"));
        item2.setQuantity(1);

        when(variantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(variant));
        when(variantRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(variant2));
        when(couponService.applyCoupon(any(), any())).thenAnswer(inv -> inv.getArgument(1));
        when(orderRepository.save(any())).thenReturn(new Order());

        checkoutService.checkout("A", "0901234567", "addr",
                List.of(cartItem, item2), null, null, null);

        assertThat(variant.getStock()).isEqualTo(8);
        assertThat(variant2.getStock()).isEqualTo(4);
        verify(variantRepository, times(2)).save(any());
    }
}
