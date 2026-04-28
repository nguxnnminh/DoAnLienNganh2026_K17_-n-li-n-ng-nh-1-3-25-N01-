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
            CouponService couponService
    ) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.couponService = couponService;
    }

    // =====================================================
    // CHECKOUT (hỗ trợ cả guest + user, có coupon)
    // =====================================================
    @Transactional
    public Order checkout(
            String customerName,
            String phone,
            String address,
            List<CartItemDTO> cart,
            User user,
            String couponCode
    ) {
        log.info("Checkout started | customer={} | user={} | coupon={}",
                customerName, user != null ? user.getEmail() : "GUEST",
                couponCode != null ? couponCode : "none");

        // 1. VALIDATE CART
        if (cart == null || cart.isEmpty()) {
            log.warn("Checkout failed: cart is empty | customer={}", customerName);
            throw new IllegalStateException("Giỏ hàng trống, vui lòng thêm sản phẩm trước khi đặt hàng.");
        }

        log.debug("Cart has {} items", cart.size());

        // 2. TẠO ORDER
        Order order = new Order();
        order.setCustomerName(customerName);
        order.setPhone(phone);
        order.setAddress(address);
        order.setStatus(OrderStatus.PENDING);

        if (user != null) {
            order.setActor(user);
        }

        // 3. CHECK STOCK + TRỪ STOCK ATOMIC (Pessimistic Lock)
        //    findByIdForUpdate → SELECT ... FOR UPDATE → lock row
        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItemDTO c : cart) {

            // LOCK ROW → tránh race condition overselling
            ProductVariant variant = variantRepository
                    .findByIdForUpdate(c.getVariantId())
                    .orElseThrow(() -> new IllegalStateException(
                    "Sản phẩm không tồn tại: variantId=" + c.getVariantId()));

            // CHECK STOCK (đang giữ lock)
            if (variant.getStock() < c.getQuantity()) {
                throw new IllegalStateException(
                        "Không đủ tồn kho cho sản phẩm: "
                        + variant.getProduct().getName()
                        + " (" + variant.getSize() + " - " + variant.getColor() + ")"
                        + ". Còn lại: " + variant.getStock()
                );
            }

            // TRỪ STOCK (vẫn đang giữ lock)
            variant.setStock(variant.getStock() - c.getQuantity());
            variant.setSold(variant.getSold() + c.getQuantity());
            variantRepository.save(variant);

            // Lấy giá HIỆN TẠI từ DB (không dùng giá cũ trong cart session)
            BigDecimal currentPrice = BigDecimal.valueOf(variant.getPrice());

            OrderItem item = new OrderItem();
            item.setProductName(c.getProductName());
            item.setSize(c.getSize());
            item.setColor(c.getColor());
            item.setPrice(currentPrice.doubleValue());
            item.setQuantity(c.getQuantity());
            item.setVariantId(c.getVariantId());
            item.setOrder(order);

            items.add(item);

            subtotal = subtotal.add(currentPrice.multiply(BigDecimal.valueOf(c.getQuantity())));
        }

        // 4. ÁP DỤNG COUPON (nếu có)
        //    couponService.applyCoupon cũng tăng usageCount trong cùng transaction
        BigDecimal total = couponService.applyCoupon(couponCode, subtotal);

        order.setItems(items);
        order.setTotal(total.doubleValue());

        // 5. SAVE
        Order savedOrder = orderRepository.save(order);

        log.info("Order created | orderId={} | subtotal={} | total={} | items={} | customer={}",
                savedOrder.getId(), subtotal, total, items.size(), customerName);

        return savedOrder;
    }

    // =====================================================
    // OVERLOAD: backward-compat (không có coupon)
    // =====================================================
    @Transactional
    public Order checkout(
            String customerName,
            String phone,
            String address,
            List<CartItemDTO> cart,
            User user
    ) {
        return checkout(customerName, phone, address, cart, user, null);
    }
}
