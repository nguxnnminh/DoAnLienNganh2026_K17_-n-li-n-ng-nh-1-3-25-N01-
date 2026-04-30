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
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final CouponService couponService;

    public CheckoutService(
            OrderRepository orderRepository,
            ProductVariantRepository variantRepository,
            CouponService couponService) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.couponService = couponService;
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
            throw new IllegalStateException("Không có sản phẩm hợp lệ trong giỏ hàng (quantity >= 1)");
        }

        for (CartItemDTO c : cartCopy) {

            // SELECT ... FOR UPDATE — prevents overselling race condition
            ProductVariant variant = variantRepository
                    .findByIdForUpdate(c.getVariantId())
                    .orElseThrow(() -> new IllegalStateException(
                    "San pham khong ton tai: variantId=" + c.getVariantId()));

            if (variant.getStock() < c.getQuantity()) {
                // Do NOT expose remaining stock count in user-facing messages
                throw new IllegalStateException(
                        "San pham '" + variant.getProduct().getName()
                        + "' (" + variant.getSize() + " / " + variant.getColor()
                        + ") khong du so luong trong kho. Vui long giam so luong.");
            }

            variant.setStock(variant.getStock() - c.getQuantity());
            variant.setSold(variant.getSold() + c.getQuantity());
            variantRepository.save(variant);

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

        // ---- Apply coupon — throws if coupon invalid at apply-time ----
        // Pass user for user-specific coupon validation
        BigDecimal total = couponService.applyCoupon(couponCode, subtotal, user);

        order.setItems(items);
        order.setTotal(total);

        Order savedOrder = orderRepository.save(order);

        log.info("Order created | orderId={} | subtotal={} | total={} | items={} | customer={}",
                savedOrder.getId(), subtotal, total, items.size(), customerName);

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
