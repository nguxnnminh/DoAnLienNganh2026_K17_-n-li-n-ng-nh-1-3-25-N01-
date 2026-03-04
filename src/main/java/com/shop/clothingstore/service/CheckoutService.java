package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

        // 1️⃣ LẤY CART (KHÔNG TRUYỀN SESSION)
        List<CartItemDTO> cart = cartService.getCart();
        if (cart.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // 2️⃣ CHECK STOCK
        for (CartItemDTO c : cart) {
            ProductVariant variant = variantRepository
                    .findById(c.getVariantId())
                    .orElseThrow(() -> new IllegalStateException("Variant not found"));

            if (variant.getStock() < c.getQuantity()) {
                throw new IllegalStateException(
                        "Not enough stock for " + variant.getProduct().getName()
                );
            }
        }

        // 3️⃣ TẠO ORDER
        Order order = new Order();
        order.setCustomerName(customerName);
        order.setPhone(phone);
        order.setAddress(address);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        if (user != null) {
            user.setFullName(customerName);
            user.setPhone(phone);
            user.setAddress(address);
            order.setActor(user);
        }

        // 4️⃣ TẠO ORDER ITEMS + TRỪ STOCK
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItemDTO c : cart) {

            ProductVariant variant = variantRepository
                    .findById(c.getVariantId())
                    .orElseThrow();

            // trừ tồn kho
            variant.setStock(variant.getStock() - c.getQuantity());
            variant.setSold(variant.getSold() + c.getQuantity());
            variantRepository.save(variant);

            OrderItem item = new OrderItem();
            item.setProductName(c.getProductName());
            item.setSize(c.getSize());
            item.setColor(c.getColor());
            item.setPrice(c.getPrice().doubleValue());
            item.setQuantity(c.getQuantity());
            item.setVariantId(c.getVariantId());
            item.setOrder(order);

            items.add(item);

            // total += price * quantity
            BigDecimal lineTotal = c.getPrice()
                    .multiply(BigDecimal.valueOf(c.getQuantity()));
            total = total.add(lineTotal);
        }

        order.setItems(items);
        order.setTotal(total.doubleValue()); // Order.total là double

        // 5️⃣ SAVE
        Order savedOrder = orderRepository.save(order);

        // 6️⃣ CLEAR CART
        session.removeAttribute("CART");

        return savedOrder;
    }
}
