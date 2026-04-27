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

import jakarta.servlet.http.HttpSession;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductVariantRepository variantRepository;

    public CheckoutService(
            OrderRepository orderRepository,
            CartService cartService,
            ProductVariantRepository variantRepository
    ) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.variantRepository = variantRepository;
    }

    @Transactional
    public Order checkout(
            String customerName,
            String phone,
            String address,
            HttpSession session,
            User user
    ) {

        log.info("Checkout started | customer={} | user={}",
                customerName, user != null ? user.getEmail() : "GUEST");

        // 1️⃣ LẤY CART
        List<CartItemDTO> cart = cartService.getCart();
        if (cart.isEmpty()) {
            log.warn("Checkout failed: cart is empty | customer={}", customerName);
            throw new IllegalStateException("Cart is empty");
        }

        log.debug("Cart has {} items", cart.size());

        // 2️⃣ TẠO ORDER
        Order order = new Order();
        order.setCustomerName(customerName);
        order.setPhone(phone);
        order.setAddress(address);
        order.setStatus(OrderStatus.PENDING);

        if (user != null) {
            order.setActor(user);
        }

        // 3️⃣ CHECK STOCK + TRỪ STOCK ATOMIC (Pessimistic Lock)
        //    Gộp check và trừ trong 1 vòng lặp duy nhất
        //    findByIdForUpdate → SELECT ... FOR UPDATE → lock row
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItemDTO c : cart) {

            // 🔒 LOCK ROW → tránh race condition
            ProductVariant variant = variantRepository
                    .findByIdForUpdate(c.getVariantId())
                    .orElseThrow(() -> new IllegalStateException(
                    "Variant not found: ID = " + c.getVariantId()));

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

            // Lấy giá HIỆN TẠI từ DB (không dùng giá cũ trong cart)
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

            BigDecimal lineTotal = currentPrice
                    .multiply(BigDecimal.valueOf(c.getQuantity()));
            total = total.add(lineTotal);
        }

        order.setItems(items);
        order.setTotal(total.doubleValue());

        // 4️⃣ SAVE
        Order savedOrder = orderRepository.save(order);

        log.info("Order created | orderId={} | total={} | items={} | customer={}",
                savedOrder.getId(), total, items.size(), customerName);

        // 5️⃣ CLEAR CART
        session.removeAttribute("CART");

        return savedOrder;
    }
}
