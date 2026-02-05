package com.shop.clothingstore.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderItem;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;

    public OrderService(OrderRepository orderRepository, ProductVariantRepository variantRepository) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();

        // Không thay đổi trạng thái nếu giống nhau
        if (oldStatus == newStatus) {
            return order;
        }

        // Kiểm tra lại tồn kho khi chuyển từ PENDING sang PROCESSING
        // (phòng trường hợp stock thay đổi sau khi checkout)
        if (newStatus == OrderStatus.PROCESSING && oldStatus == OrderStatus.PENDING) {
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = variantRepository.findById(item.getVariantId())
                        .orElseThrow(() -> new RuntimeException(
                                "Variant không tồn tại: ID = " + item.getVariantId()));

                if (variant.getStock() < item.getQuantity()) {
                    throw new IllegalStateException(
                            "Không đủ tồn kho cho sản phẩm: " + item.getProductName() +
                            " (" + item.getSize() + " - " + item.getColor() + ")" +
                            ". Tồn kho hiện tại: " + variant.getStock() + ", cần: " + item.getQuantity());
                }

                // Vì checkout đã trừ stock rồi, ở đây chỉ kiểm tra, không trừ lại
                // Nếu bạn muốn trừ lại ở bước PROCESSING, uncomment dòng dưới
                // variant.setStock(variant.getStock() - item.getQuantity());
                // variantRepository.save(variant);
            }
        }

        // Hoàn tồn kho khi hủy đơn (chuyển sang CANCELLED)
        if (newStatus == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = variantRepository.findById(item.getVariantId())
                        .orElseThrow(() -> new RuntimeException(
                                "Variant không tồn tại: ID = " + item.getVariantId()));

                variant.setStock(variant.getStock() + item.getQuantity());
                variantRepository.save(variant);
            }
        }

        // Cập nhật trạng thái đơn hàng
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }
}