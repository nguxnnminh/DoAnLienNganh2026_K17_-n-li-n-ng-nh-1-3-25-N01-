package com.shop.clothingstore.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // =====================================================
    // USER QUERIES
    // =====================================================

    List<Order> findByUser(User user);

    List<Order> findByUserOrderByCreatedAtDesc(User user);


    // =====================================================
    // ADMIN QUERIES
    // =====================================================

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    long countByStatus(OrderStatus status);

    long countByStatusAndStatusNotNull(OrderStatus status);


    // =====================================================
    // DASHBOARD QUERIES
    // =====================================================

    // ⭐ Tổng doanh thu theo trạng thái
    @Query("""
        SELECT COALESCE(SUM(o.total), 0)
        FROM Order o
        WHERE o.status = :status
    """)
    BigDecimal getTotalRevenueByStatus(@Param("status") OrderStatus status);


    // ⭐ 5 đơn hàng mới nhất (Dashboard)
    List<Order> findTop5ByOrderByCreatedAtDesc();

}
