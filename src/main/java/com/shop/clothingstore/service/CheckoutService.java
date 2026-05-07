package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.dto.CartItemDTO;
import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderItem;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.exception.OutOfStockException;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    public static final BigDecimal FREE_SHIP_THRESHOLD = BigDecimal.valueOf(500_000);
    public static final BigDecimal SHIP_FEE            = BigDecimal.valueOf(30_000);

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final CouponService couponService;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public CheckoutService(
            OrderRepository orderRepository,
            ProductVariantRepository variantRepository,
            CouponService couponService,
            ProductRepository productRepository,
            PaymentService paymentService,
            NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.couponService = couponService;
        this.productRepository = productRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    // =====================================================
    // CHECKOUT (supports guest + authenticated, with coupon)
    // =====================================================
    @Transactional
    public Order checkout(
            String customerName,
            String phone,
            String address,
            List<CartItemDTO> cart,
            User user,
            String couponCode,
            String note) {

        log.info("Checkout started | customer={} | user={} | coupon={}",
                customerName,
                user != null ? user.getEmail() : "GUEST",
                couponCode != null ? couponCode : "none");

        if (cart == null || cart.isEmpty()) {
            throw new IllegalStateException("Gio hang trong. Vui long them san pham truoc khi dat hang.");
        }

        // Defensive copy — never mutate the caller's list
        List<CartItemDTO> cartCopy = new ArrayList<>(cart);

        Order order = new Order();
        order.setCustomerName(customerName);
        order.setPhone(phone);
        order.setAddress(address);
        if (note != null && !note.isBlank()) {
            order.setNote(note.trim());
        }
        order.setStatus(OrderStatus.PENDING);

        if (user != null) {
            order.setActor(user);
        }

        // ---- Stock check + deduction (pessimistic lock per variant) ----
        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        // Reject zero/negative quantity items (data integrity)
        cartCopy.removeIf(c -> c.getQuantity() <= 0);
        if (cartCopy.isEmpty()) {
            throw new IllegalStateException("No valid items in cart (quantity >= 1)");
        }

        // Collect product IDs touched during this checkout — used for a single
        // batch totalSold refresh AFTER the loop, avoiding one findById+save per item.
        java.util.Set<Long> touchedProductIds = new java.util.LinkedHashSet<>();

        for (CartItemDTO c : cartCopy) {

            // SELECT ... FOR UPDATE — prevents overselling race condition
            ProductVariant variant = variantRepository
                    .findByIdForUpdate(c.getVariantId())
                    .orElseThrow(() -> new IllegalStateException(
                    "Product variant not found: variantId=" + c.getVariantId()));

            if (variant.getStock() < c.getQuantity()) {
                throw new OutOfStockException(
                        variant.getProduct().getName(),
                        variant.getSize(),
                        variant.getColor());
            }

            variant.setStock(variant.getStock() - c.getQuantity());
            variant.setSold(variant.getSold() + c.getQuantity());
            variantRepository.save(variant);

            touchedProductIds.add(variant.getProduct().getId());

            // Price snapshot from DB — never trust the cart-session price
            BigDecimal currentPrice = variant.getPrice();

            OrderItem item = new OrderItem();
            item.setProductName(c.getProductName());
            item.setSize(c.getSize());
            item.setColor(c.getColor());
            item.setPrice(currentPrice);
            item.setQuantity(c.getQuantity());
            item.setVariantId(c.getVariantId());
            item.setOrder(order);

            items.add(item);
            subtotal = subtotal.add(currentPrice.multiply(BigDecimal.valueOf(c.getQuantity())));
        }

        // Batch-refresh denormalized totalSold — one findById+save per unique product,
        // not one per cart item. Keeps ORDER BY total_sold correct without N×2 queries.
        for (Long pid : touchedProductIds) {
            if (pid == null) continue;
            productRepository.findById(pid).ifPresent(p -> {
                p.refreshTotalSold();
                productRepository.save(p);
            });
        }

        // ---- Shipping fee: free on orders >= 500k, else flat 30k ----
        BigDecimal shippingFee = subtotal.compareTo(FREE_SHIP_THRESHOLD) >= 0
                ? BigDecimal.ZERO : SHIP_FEE;

        // ---- Apply coupon — throws if coupon invalid at apply-time ----
        BigDecimal discountedSubtotal = couponService.applyCoupon(couponCode, subtotal, user);
        BigDecimal total = discountedSubtotal.add(shippingFee);

        order.setItems(items);
        order.setShippingFee(shippingFee);
        order.setTotal(total);

        Order savedOrder = orderRepository.save(order);

        log.info("Order created | orderId={} | subtotal={} | total={} | items={} | customer={}",
                savedOrder.getId(), subtotal, total, items.size(), customerName);

        // Create COD payment record and notify authenticated user
        paymentService.createForOrder(savedOrder, PaymentService.METHOD_COD);
        if (user != null) {
            notificationService.notifyOrderPlaced(user, savedOrder);
        }

        return savedOrder;
    }

    // Backward-compatible overload without coupon or note
    @Transactional
    public Order checkout(
            String customerName,
            String phone,
            String address,
            List<CartItemDTO> cart,
            User user) {
        return checkout(customerName, phone, address, cart, user, null, null);
    }
}
