package com.shop.clothingstore.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import com.shop.clothingstore.exception.InvalidOrderStateException;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductRepository;
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
    // ADMIN-driven transitions (used by updateOrderStatus)
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING,           EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING,        EnumSet.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
            OrderStatus.CANCEL_REQUESTED,  EnumSet.of(OrderStatus.CANCELLED, OrderStatus.PROCESSING),
            OrderStatus.SHIPPING,          EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED),
            OrderStatus.COMPLETED,         EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED,         EnumSet.noneOf(OrderStatus.class)
    );

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ShipmentService shipmentService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository,
            ProductVariantRepository variantRepository,
            ProductRepository productRepository,
            ShipmentService shipmentService,
            PaymentService paymentService,
            NotificationService notificationService) {
        super(orderRepository);
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.shipmentService = shipmentService;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    // =====================================================
    // GET ALL ORDERS (PAGINATED)
    // =====================================================
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // =====================================================
    // ADMIN SEARCH (keyword + status + date range)
    // All params are optional — passing null means "no filter".
    // =====================================================
    public Page<Order> searchOrders(
            String keyword,
            String statusStr,
            String dateFromStr,
            String dateToStr,
            Pageable pageable) {

        OrderStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = OrderStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        LocalDateTime dateFrom = null;
        LocalDateTime dateTo   = null;
        if (dateFromStr != null && !dateFromStr.isBlank()) {
            try {
                dateFrom = LocalDate.parse(dateFromStr).atStartOfDay();
            } catch (Exception ignored) {}
        }
        if (dateToStr != null && !dateToStr.isBlank()) {
            try {
                dateTo = LocalDate.parse(dateToStr).atTime(LocalTime.MAX);
            } catch (Exception ignored) {}
        }

        // If the keyword is a pure number, treat it as an order ID search
        Long orderId = null;
        String kw    = null;
        if (keyword != null && !keyword.isBlank()) {
            try {
                orderId = Long.parseLong(keyword.trim());
                // Don't pass as text keyword — match by ID only
            } catch (NumberFormatException e) {
                kw = keyword.trim();  // text search by name / phone
            }
        }

        return orderRepository.searchAdmin(kw, orderId, status, dateFrom, dateTo, pageable);
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
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderStatus oldStatus = order.getStatus();

        if (oldStatus == newStatus) {
            return order;
        }

        // Guard: enforce state machine rules
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(oldStatus, EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new InvalidOrderStateException(oldStatus, newStatus, allowed);
        }

        log.info("Order status change | orderId={} | {} -> {}", orderId, oldStatus, newStatus);

        // Restore stock when cancelling a non-cancelled order
        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Long variantId = item.getVariantId();
                if (variantId == null) continue;
                variantRepository.findById(variantId).ifPresent(variant -> {
                    variant.setStock(variant.getStock() + item.getQuantity());
                    variant.setSold(Math.max(0, variant.getSold() - item.getQuantity()));
                    variantRepository.save(variant);
                    log.debug("Stock restored | variantId={} | +{} units", variantId, item.getQuantity());
                    Long productId = variant.getProduct().getId();
                    if (productId == null) return;
                    productRepository.findById(productId).ifPresent(product -> {
                        product.refreshTotalSold();
                        productRepository.save(product);
                    });
                });
            }
        }

        order.setStatus(newStatus);
        Order saved = save(order);

        // Sync shipment lifecycle
        shipmentService.syncWithOrderStatus(saved, newStatus);

        // Payment: mark paid when completed, refunded when cancelled
        if (newStatus == OrderStatus.COMPLETED) {
            paymentService.markPaid(saved);
        } else if (newStatus == OrderStatus.CANCELLED) {
            paymentService.markRefunded(saved);
        }

        // In-app notification for authenticated customers.
        // Skip for CANCEL_REQUESTED transitions — acceptCancelRequest/denyCancelRequest
        // send their own specific notifications to avoid duplicates.
        User customer = saved.getActor();
        if (customer != null && oldStatus != OrderStatus.CANCEL_REQUESTED) {
            notificationService.notifyOrderStatusChanged(customer, saved);
        }

        log.info("Order status updated | orderId={} | newStatus={}", orderId, newStatus);
        return saved;
    }

    // =====================================================
    // FIND ORDERS BY USER
    // =====================================================
    public List<Order> findOrdersByUser(User user) {
        return orderRepository.findByActorOrderByCreatedAtDesc(user);
    }

    // =====================================================
    // USER: Self-cancel (PENDING only, authenticated)
    // Stock is restored via the standard CANCELLED path.
    // =====================================================
    @Transactional
    public Order selfCancel(Long orderId, User user) {
        Order order = findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getActor() == null || !order.getActor().getId().equals(user.getId())) {
            throw new IllegalStateException("You do not have permission to cancel this order.");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Orders can only be self-cancelled while in PENDING status.");
        }

        return updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    // =====================================================
    // USER: Request cancellation (PROCESSING only)
    // Transitions to CANCEL_REQUESTED. Stock is NOT restored yet.
    // =====================================================
    @Transactional
    public Order requestCancel(Long orderId, User user, String reason) {
        Order order = findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getActor() == null || !order.getActor().getId().equals(user.getId())) {
            throw new IllegalStateException("You do not have permission to request cancellation for this order.");
        }
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Cancellation can only be requested while the order is in PROCESSING status.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Please provide a cancellation reason.");
        }

        order.setCancelReason(reason.trim());
        order.setStatus(OrderStatus.CANCEL_REQUESTED);
        Order saved = orderRepository.save(order);

        notificationService.notifyCancelRequested(user, saved);
        log.info("Cancel requested | orderId={} | user={} | reason={}", orderId, user.getEmail(), reason);
        return saved;
    }

    // =====================================================
    // ADMIN: Accept cancel request → CANCELLED + restore stock
    // =====================================================
    @Transactional
    public Order acceptCancelRequest(Long orderId) {
        Order order = findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("This order has no pending cancellation request.");
        }

        Order cancelled = updateOrderStatus(orderId, OrderStatus.CANCELLED);

        User customer = cancelled.getActor();
        if (customer != null) {
            notificationService.notifyCancelAccepted(customer, cancelled);
        }
        log.info("Cancel request accepted | orderId={}", orderId);
        return cancelled;
    }

    // =====================================================
    // ADMIN: Deny cancel request → back to PROCESSING
    // =====================================================
    @Transactional
    public Order denyCancelRequest(Long orderId) {
        Order order = findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            throw new IllegalStateException("This order has no pending cancellation request.");
        }

        order.setCancelReason(null);
        order.setStatus(OrderStatus.PROCESSING);
        Order saved = orderRepository.save(order);

        User customer = saved.getActor();
        if (customer != null) {
            notificationService.notifyCancelDenied(customer, saved);
        }
        log.info("Cancel request denied | orderId={}", orderId);
        return saved;
    }
}
