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

    // ===============================
    // USER
    // ===============================

    List<Order> findByUser(User user);

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    // ===============================
    // ADMIN
    // ===============================

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    long countByStatus(OrderStatus status);
    long countByStatusAndStatusNotNull(OrderStatus status);

    // ===============================
    // DASHBOARD
    // ===============================

    @Query("""
        select coalesce(sum(o.total), 0)
        from Order o
        where o.status = :status
    """)
    BigDecimal getTotalRevenueByStatus(@Param("status") OrderStatus status);
}
