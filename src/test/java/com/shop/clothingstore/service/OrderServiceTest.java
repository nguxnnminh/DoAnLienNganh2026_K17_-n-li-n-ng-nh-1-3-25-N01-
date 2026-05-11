package com.shop.clothingstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

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

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderItem;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.exception.InvalidOrderStateException;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ShipmentService shipmentService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private ProductVariant variant;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        variant = new ProductVariant();
        variant.setStock(5);
        variant.setSold(3);
        variant.setPrice(new BigDecimal("200000"));

        orderItem = new OrderItem();
        orderItem.setVariantId(10L);
        orderItem.setQuantity(2);
        orderItem.setPrice(new BigDecimal("200000"));
        orderItem.setProductName("Test Product");

        order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setTotal(new BigDecimal("400000"));
        List<OrderItem> items = new ArrayList<>();
        items.add(orderItem);
        order.setItems(items);

        lenient().when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // =====================================================
    // STATE MACHINE — valid transitions
    // =====================================================

    @Test
    void updateStatus_pendingToProcessing_succeeds() {
        Order result = orderService.updateOrderStatus(1L, OrderStatus.PROCESSING);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void updateStatus_processingToShipping_succeeds() {
        order.setStatus(OrderStatus.PROCESSING);
        Order result = orderService.updateOrderStatus(1L, OrderStatus.SHIPPING);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    void updateStatus_shippingToCompleted_succeeds() {
        order.setStatus(OrderStatus.SHIPPING);
        Order result = orderService.updateOrderStatus(1L, OrderStatus.COMPLETED);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void updateStatus_pendingToCancelled_succeeds() {
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));
        Order result = orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    // =====================================================
    // STATE MACHINE — invalid transitions must be rejected
    // =====================================================

    @Test
    void updateStatus_completedToPending_throwsIllegalState() {
        order.setStatus(OrderStatus.COMPLETED);
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PENDING))
                .isInstanceOf(InvalidOrderStateException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatus_completedToProcessing_throwsIllegalState() {
        order.setStatus(OrderStatus.COMPLETED);
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PROCESSING))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void updateStatus_cancelledToProcessing_throwsIllegalState() {
        order.setStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PROCESSING))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void updateStatus_shippingToPending_throwsIllegalState() {
        order.setStatus(OrderStatus.SHIPPING);
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PENDING))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void updateStatus_processingToPending_throwsIllegalState() {
        order.setStatus(OrderStatus.PROCESSING);
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PENDING))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    // =====================================================
    // SAME STATUS — no-op, no save
    // =====================================================

    @Test
    void updateStatus_sameStatus_returnsOrderWithoutSaving() {
        Order result = orderService.updateOrderStatus(1L, OrderStatus.PENDING);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, never()).save(any());
    }

    // =====================================================
    // STOCK RESTORATION on CANCEL
    // =====================================================

    @Test
    void updateStatus_cancelFromPending_restoresStock() {
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));

        orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

        // stock restored: 5 + 2 = 7
        assertThat(variant.getStock()).isEqualTo(7);
        // sold decremented: 3 - 2 = 1
        assertThat(variant.getSold()).isEqualTo(1);
        verify(variantRepository).save(variant);
    }

    @Test
    void updateStatus_cancelFromShipping_restoresStock() {
        order.setStatus(OrderStatus.SHIPPING);
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));

        orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

        assertThat(variant.getStock()).isEqualTo(7);
        assertThat(variant.getSold()).isEqualTo(1);
    }

    @Test
    void updateStatus_nonCancelTransition_doesNotTouchStock() {
        orderService.updateOrderStatus(1L, OrderStatus.PROCESSING);
        verify(variantRepository, never()).save(any());
        verify(variantRepository, never()).findById(any());
    }

    @Test
    void updateStatus_soldCannotGoBelowZero() {
        variant.setSold(0); // already zero
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));

        orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

        // Math.max(0, 0 - 2) = 0, not negative
        assertThat(variant.getSold()).isEqualTo(0);
    }

    // =====================================================
    // ORDER NOT FOUND
    // =====================================================

    @Test
    void updateStatus_orderNotFound_throwsRuntimeException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(999L, OrderStatus.PROCESSING))
                .isInstanceOf(RuntimeException.class);
    }
}
