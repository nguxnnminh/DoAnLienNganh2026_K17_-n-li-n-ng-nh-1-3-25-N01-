package com.shop.clothingstore.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderItem;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.Review;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.OrderItemRepository;
import com.shop.clothingstore.repository.ReviewRepository;
import com.shop.clothingstore.repository.UserRepository;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository) {

        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    // ======================================================
    // CREATE REVIEW
    // ======================================================
    @Transactional
    public Long createReview(
            Long actorId,
            Long orderItemId,
            double rating,
            String comment) {

        // VALIDATE RATING
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating không hợp lệ.");
        }

        // CHECK REVIEW EXIST
        if (reviewRepository.findByOrderItemId(orderItemId).isPresent()) {
            throw new IllegalStateException("Item này đã được đánh giá.");
        }

        // LOAD ORDER ITEM
        OrderItem orderItem = orderItemRepository
                .findById(orderItemId)
                .orElseThrow(()
                        -> new IllegalStateException("Order item không tồn tại."));

        Order order = orderItem.getOrder();

        // CHECK OWNER
        if (order.getActor() == null
                || !order.getActor().getId().equals(actorId)) {

            throw new IllegalStateException(
                    "Bạn không có quyền đánh giá item này.");
        }

        // CHECK COMPLETED
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Chỉ có thể đánh giá khi giao dịch hoàn tất.");
        }

        User user = userRepository.findById(actorId)
                .orElseThrow(()
                        -> new IllegalStateException("User không tồn tại."));

        // CREATE REVIEW
        Review review = new Review();
        review.setRating(rating);
        review.setComment(comment);
        review.setActor(user);
        review.setOrderItem(orderItem);
        review.setItemId(orderItem.getVariantId());
        reviewRepository.save(review);

        return order.getId();
    }

    // ======================================================
    // RATING INFO
    // ======================================================
    public double getAverageRating(Long itemId) {

        Double avg = reviewRepository.getAverageRatingByItemId(itemId);

        return avg != null ? avg : 0.0;
    }

    public long getReviewCount(Long itemId) {

        return reviewRepository.countByItemId(itemId);
    }

    public List<Review> getReviewsByItem(Long itemId) {

        return reviewRepository
                .findAllByItemIdOrderByCreatedAtDesc(itemId);
    }

    public boolean hasReviewByOrderItem(Long orderItemId) {

        return reviewRepository
                .findByOrderItemId(orderItemId)
                .isPresent();
    }
}
