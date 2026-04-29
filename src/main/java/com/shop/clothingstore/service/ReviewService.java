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

    @Transactional
    public Long createReview(Long actorId, Long orderItemId, double rating, String comment) {

        java.util.Objects.requireNonNull(actorId, "actorId must not be null");
        java.util.Objects.requireNonNull(orderItemId, "orderItemId must not be null");

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating phai tu 1 den 5.");
        }

        if (reviewRepository.findByOrderItem_Id(orderItemId).isPresent()) {
            throw new IllegalStateException("Item nay da duoc danh gia.");
        }

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalStateException("Order item khong ton tai."));

        Order order = orderItem.getOrder();

        if (order.getActor() == null || !order.getActor().getId().equals(actorId)) {
            throw new IllegalStateException("Ban khong co quyen danh gia item nay.");
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Chi co the danh gia khi giao dich hoan tat.");
        }

        User user = userRepository.findById(java.util.Objects.requireNonNull(actorId))
                .orElseThrow(() -> new IllegalStateException("User khong ton tai."));

        Review review = new Review();
        review.setRating(rating);
        review.setComment(comment);
        review.setActor(user);
        review.setOrderItem(orderItem);
        review.setItemId(orderItem.getVariantId());
        reviewRepository.save(review);

        return order.getId();
    }

    public double getAverageRating(Long itemId) {
        Double avg = reviewRepository.getAverageRatingByItemId(itemId);
        return avg != null ? avg : 0.0;
    }

    public long getReviewCount(Long itemId) {
        return reviewRepository.countByItemId(itemId);
    }

    public List<Review> getReviewsByItem(Long itemId) {
        return reviewRepository.findAllByItemIdOrderByCreatedAtDesc(itemId);
    }

    public boolean hasReviewByOrderItem(Long orderItemId) {
        return reviewRepository.findByOrderItem_Id(orderItemId).isPresent();
    }
}
