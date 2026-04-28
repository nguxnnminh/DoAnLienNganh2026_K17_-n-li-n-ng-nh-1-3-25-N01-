package com.shop.clothingstore.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<Order> findByActorOrderByCreatedAtDesc(User actor, Pageable pageable);

    // =====================================================
    // ADMIN QUERIES
    // =====================================================
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    long countByStatus(OrderStatus status);

    // =====================================================
    // DASHBOARD KPI (GENERIC TOTAL AMOUNT)
    // =====================================================
    @Query("""
        SELECT COALESCE(SUM(o.total), 0)
        FROM Order o
        WHERE o.status = :status
    """)
    BigDecimal getTotalAmountByStatus(@Param("status") OrderStatus status);

    List<Order> findTop5ByOrderByCreatedAtDesc();

    @Query("""
        SELECT o FROM Order o
        WHERE o.status = :status
          AND o.createdAt >= :since
        ORDER BY o.createdAt DESC
    """)
    List<Order> findByStatusSince(
            @Param("status") OrderStatus status,
            @Param("since") java.time.LocalDateTime since
    );

    // =====================================================
    // DASHBOARD CHART (GENERIC AGGREGATION)
    // =====================================================
    @Query("""
        SELECT DATE(o.createdAt), SUM(o.total)
        FROM Order o
        WHERE o.status = :status
        GROUP BY DATE(o.createdAt)
        ORDER BY DATE(o.createdAt)
    """)
    List<Object[]> getTotalAmountByDate(@Param("status") OrderStatus status);

    @Query("""
        SELECT DATE(o.createdAt), SUM(o.total)
        FROM Order o
        WHERE o.status = :status
          AND o.createdAt >= :startDate
        GROUP BY DATE(o.createdAt)
        ORDER BY DATE(o.createdAt)
    """)
    List<Object[]> getTotalAmountSince(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDate startDate
    );

    // =====================================================
    // GENERIC TRANSACTION CHECK (NO PRODUCT COUPLING)
    // =====================================================
    @Query("""
        SELECT COUNT(o) > 0
        FROM Order o
        JOIN o.items i
        WHERE o.actor.id = :actorId
          AND o.status = :status
          AND i.variantId = :variantId
    """)
    boolean existsCompletedTransactionWithVariant(
            @Param("actorId") Long actorId,
            @Param("status") OrderStatus status,
            @Param("variantId") Long variantId
    );
}
