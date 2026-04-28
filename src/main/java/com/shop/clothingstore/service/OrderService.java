package com.shop.clothingstore.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderItem;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;
import com.shop.clothingstore.service.base.GenericServiceBase;

@Service
@SuppressWarnings("null")
public class OrderService extends GenericServiceBase<Order, Long> {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;

    public OrderService(OrderRepository orderRepository,
            ProductVariantRepository variantRepository) {

        super(orderRepository);
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
    }

    // =====================================================
    // GET ALL ORDERS (PAGINATED)
    // =====================================================
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // =====================================================
    // UPDATE ORDER STATUS
    // BUG FIX: Đã xóa block "CHECK STOCK BEFORE PROCESSING"
    // vì stock đã được trừ tại thời điểm checkout (CheckoutService).
    // Việc check lại sau khi stock đã giảm sẽ luôn fail sai.
    // =====================================================
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {

        Order order = findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        OrderStatus oldStatus = order.getStatus();

        if (oldStatus == newStatus) {
            return order;
        }

        log.info("Order status change | orderId={} | {} → {}", orderId, oldStatus, newStatus);

        // ===== RESTORE STOCK WHEN CANCELLED =====
        // Chỉ hoàn stock khi cancel đơn chưa bị cancel trước đó
        if (newStatus == OrderStatus.CANCELLED
                && oldStatus != OrderStatus.CANCELLED) {

            for (OrderItem item : order.getItems()) {

                ProductVariant variant = variantRepository.findById(item.getVariantId())
                        .orElseThrow(() -> new RuntimeException(
                        "Variant không tồn tại: ID = " + item.getVariantId()));

                variant.setStock(variant.getStock() + item.getQuantity());
                variant.setSold(Math.max(0, variant.getSold() - item.getQuantity()));
                variantRepository.save(variant);

                log.debug("Stock restored | variantId={} | +{} units",
                        item.getVariantId(), item.getQuantity());
            }
        }

        order.setStatus(newStatus);

        log.info("Order status updated | orderId={} | newStatus={}", orderId, newStatus);

        return save(order);
    }

    // =====================================================
    // FIND ORDERS BY USER
    // =====================================================
    public List<Order> findOrdersByUser(User user) {
        return orderRepository.findByActorOrderByCreatedAtDesc(user);
    }
}
