package com.shop.clothingstore.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.clothingstore.entity.Review;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface ReviewRepository extends BaseRepository<Review, Long> {

    Optional<Review> findByOrderItem_Id(Long orderItemId);

    @Query("""
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.itemId = :itemId
    """)
    Double getAverageRatingByItemId(@Param("itemId") Long itemId);

    @Query("""
        SELECT COUNT(r)
        FROM Review r
        WHERE r.itemId = :itemId
    """)
    long countByItemId(@Param("itemId") Long itemId);

    @Query("""
        SELECT r
        FROM Review r
        WHERE r.itemId = :itemId
        ORDER BY r.createdAt DESC
    """)
    List<Review> findAllByItemIdOrderByCreatedAtDesc(@Param("itemId") Long itemId);
}
