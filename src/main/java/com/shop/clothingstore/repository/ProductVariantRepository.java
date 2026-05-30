package com.shop.clothingstore.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.repository.base.BaseRepository;

import jakarta.persistence.LockModeType;

public interface ProductVariantRepository extends BaseRepository<ProductVariant, Long> {

    List<ProductVariant> findByProduct(Product product);

    Optional<ProductVariant> findByProductAndSizeAndColor(Product product, String size, String color);

    /**
     * Pessimistic lock: SELECT ... FOR UPDATE Dùng khi checkout để tránh race
     * condition overselling
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);

    // =====================================================
    // DASHBOARD QUERIES
    // =====================================================
    long countByStockLessThan(int stock);

    List<ProductVariant> findByStockLessThan(int stock);

    @Query("SELECT SUM(pv.stock) FROM ProductVariant pv")
    Long getTotalStock();

    @Query("SELECT SUM(pv.sold) FROM ProductVariant pv")
    Long getTotalSold();

}
