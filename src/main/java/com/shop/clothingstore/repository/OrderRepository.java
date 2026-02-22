package com.shop.clothingstore.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface OrderRepository extends BaseRepository<Order, Long> {

    // =====================================================
    // USER QUERIES
    // =====================================================

    List<Order> findByActor(User actor);

    List<Order> findByActorOrderByCreatedAtDesc(User actor);


    // =====================================================
    // ADMIN QUERIES
    // =====================================================

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    long countByStatus(OrderStatus status);


    // =====================================================
    // DASHBOARD KPI
    // =====================================================

    @Query("""
        SELECT COALESCE(SUM(o.total), 0)
        FROM Order o
        WHERE o.status = :status
    """)
    BigDecimal getTotalRevenueByStatus(@Param("status") OrderStatus status);


    List<Order> findTop5ByOrderByCreatedAtDesc();


    // =====================================================
    // DASHBOARD CHART
    // =====================================================

    @Query("""
        SELECT DATE(o.createdAt), SUM(o.total)
        FROM Order o
        WHERE o.status = :status
        GROUP BY DATE(o.createdAt)
        ORDER BY DATE(o.createdAt)
    """)
    List<Object[]> getRevenueByDate(@Param("status") OrderStatus status);


    @Query("""
        SELECT DATE(o.createdAt), SUM(o.total)
        FROM Order o
        WHERE o.status = :status
        AND o.createdAt >= :startDate
        GROUP BY DATE(o.createdAt)
        ORDER BY DATE(o.createdAt)
    """)
    List<Object[]> getRevenueSince(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDate startDate
    );
}