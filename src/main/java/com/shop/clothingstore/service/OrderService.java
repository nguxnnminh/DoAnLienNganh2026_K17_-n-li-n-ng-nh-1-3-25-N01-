package com.shop.clothingstore.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderItem;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;
import com.shop.clothingstore.service.base.GenericServiceBase;

@Service
public class OrderService extends GenericServiceBase<Order, Long> {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    // =====================================================
    // STATE MACHINE — valid forward transitions
    // CANCELLED and COMPLETED are terminal states.
    // Prevents stock corruption from arbitrary admin clicks.
    // =====================================================
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING,     EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING,  EnumSet.of(OrderStatus.SHIPPING,   OrderStatus.CANCELLED),
            OrderStatus.SHIPPING,    EnumSet.of(OrderStatus.COMPLETED,  OrderStatus.CANCELLED),
            OrderStatus.COMPLETED,   EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED,   EnumSet.noneOf(OrderStatus.class)
    );

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
    // Enforces state machine — invalid transitions are rejected.
    // Stock is restored only when transitioning TO CANCELLED
    // from a state that is not already CANCELLED.
    // =====================================================
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {

        Order order = findById(orderId)
                .orElseThrow(() -> new RuntimeException("Don hang khong ton tai"));

        OrderStatus oldStatus = order.getStatus();

        if (oldStatus == newStatus) {
            return order;
        }

        // Guard: enforce state machine rules
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(oldStatus, EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Khong the chuyen trang thai don hang tu " + oldStatus + " sang " + newStatus + ". "
                    + "Cac trang thai hop le: " + allowed);
        }

        log.info("Order status change | orderId={} | {} -> {}", orderId, oldStatus, newStatus);

        // Restore stock when cancelling a non-cancelled order
        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                variantRepository.findById(item.getVariantId()).ifPresent(variant -> {
                    variant.setStock(variant.getStock() + item.getQuantity());
                    variant.setSold(Math.max(0, variant.getSold() - item.getQuantity()));
                    variantRepository.save(variant);
                    log.debug("Stock restored | variantId={} | +{} units",
                            item.getVariantId(), item.getQuantity());
                });
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
