package com.shop.clothingstore.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.clothingstore.entity.Review;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface ReviewRepository extends BaseRepository<Review, Long> {

    // =========================================
    // CHECK ĐÃ REVIEW ORDER ITEM CHƯA
    // =========================================
    Optional<Review> findByOrderItemId(Long orderItemId);

    // =========================================
    // AVERAGE RATING
    // =========================================
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    // =========================================
    // COUNT REVIEW
    // =========================================
    long countByProductId(Long productId);

    // =========================================
    // LIST REVIEW (mới nhất trước)
    // =========================================
    List<Review> findAllByProductIdOrderByCreatedAtDesc(Long productId);
}
