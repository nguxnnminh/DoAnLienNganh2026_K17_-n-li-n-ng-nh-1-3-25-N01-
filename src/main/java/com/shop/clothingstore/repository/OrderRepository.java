package com.shop.clothingstore.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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


    // =====================================================
    // DASHBOARD KPI
    // =====================================================

    /**
     * ⭐ Tổng doanh thu theo trạng thái
     */
    @Query("""
        SELECT COALESCE(SUM(o.total), 0)
        FROM Order o
        WHERE o.status = :status
    """)
    BigDecimal getTotalRevenueByStatus(@Param("status") OrderStatus status);


    /**
     * ⭐ 5 đơn hàng mới nhất
     */
    List<Order> findTop5ByOrderByCreatedAtDesc();


    // =====================================================
    // DASHBOARD CHART
    // =====================================================

    /**
     * ⭐ Doanh thu theo ngày
     *
     * return:
     *   Object[0] = java.sql.Date
     *   Object[1] = Double (SUM)
     */
    @Query("""
        SELECT DATE(o.createdAt), SUM(o.total)
        FROM Order o
        WHERE o.status = :status
        GROUP BY DATE(o.createdAt)
        ORDER BY DATE(o.createdAt)
    """)
    List<Object[]> getRevenueByDate(@Param("status") OrderStatus status);


    /**
     * ⭐ Doanh thu từ ngày X
     */
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
