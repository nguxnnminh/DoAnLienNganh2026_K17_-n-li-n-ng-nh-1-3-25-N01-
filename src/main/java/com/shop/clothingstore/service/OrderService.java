package com.shop.clothingstore.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderItem;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;
import com.shop.clothingstore.service.base.GenericServiceBase;

import jakarta.transaction.Transactional;

@Service
public class OrderService
        extends GenericServiceBase<Order, Long> {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;

    public OrderService(OrderRepository orderRepository,
            ProductVariantRepository variantRepository) {

        super(orderRepository);   // 🔥 QUAN TRỌNG

        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
    }

    // =====================================================
    // GET ALL ORDERS (CUSTOM SORT)
    // =====================================================
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    // =====================================================
    // UPDATE ORDER STATUS
    // =====================================================
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {

        Order order = findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        OrderStatus oldStatus = order.getStatus();

        if (oldStatus == newStatus) {
            return order;
        }

        // ===== CHECK STOCK BEFORE PROCESSING =====
        if (newStatus == OrderStatus.PROCESSING
                && oldStatus == OrderStatus.PENDING) {

            for (OrderItem item : order.getItems()) {

                ProductVariant variant = variantRepository.findById(item.getVariantId())
                        .orElseThrow(() -> new RuntimeException(
                        "Variant không tồn tại: ID = " + item.getVariantId()));

                if (variant.getStock() < item.getQuantity()) {
                    throw new IllegalStateException(
                            "Không đủ tồn kho cho sản phẩm: "
                            + item.getProductName()
                            + " (" + item.getSize()
                            + " - " + item.getColor()
                            + ")"
                            + ". Tồn kho hiện tại: "
                            + variant.getStock()
                            + ", cần: "
                            + item.getQuantity());
                }
            }
        }

        // ===== RESTORE STOCK WHEN CANCELLED =====
        if (newStatus == OrderStatus.CANCELLED
                && oldStatus != OrderStatus.CANCELLED) {

            for (OrderItem item : order.getItems()) {

                ProductVariant variant = variantRepository.findById(item.getVariantId())
                        .orElseThrow(() -> new RuntimeException(
                        "Variant không tồn tại: ID = " + item.getVariantId()));

                variant.setStock(
                        variant.getStock() + item.getQuantity()
                );

                variant.setSold(
                        variant.getSold() - item.getQuantity()
                );

                variantRepository.save(variant);

                variantRepository.save(variant);
            }
        }

        order.setStatus(newStatus);

        return save(order);   // 🔥 dùng GenericServiceBase
    }

    // =====================================================
    // FIND ORDERS BY USER
    // =====================================================
    public List<Order> findOrdersByUser(User user) {
        return orderRepository.findByActorOrderByCreatedAtDesc(user);
    }
}
