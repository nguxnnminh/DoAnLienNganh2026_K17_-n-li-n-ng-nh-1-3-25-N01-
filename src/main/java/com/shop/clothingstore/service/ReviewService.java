package com.shop.clothingstore.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderItem;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.entity.Review;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.OrderItemRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;
import com.shop.clothingstore.repository.ReviewRepository;
import com.shop.clothingstore.repository.UserRepository;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            OrderItemRepository orderItemRepository,
            ProductVariantRepository variantRepository,
            UserRepository userRepository) {

        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
    }

    // ======================================================
    // CREATE REVIEW THEO ORDER ITEM
    // ======================================================
    @Transactional
    public Long createReview(Long userId,
            Long orderItemId,
            double rating,
            String comment) {

        // 1️⃣ Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating không hợp lệ.");
        }

        // 2️⃣ Check đã review chưa
        if (reviewRepository.findByOrderItemId(orderItemId).isPresent()) {
            throw new IllegalStateException("Sản phẩm này đã được đánh giá.");
        }

        // 3️⃣ Lấy order item
        OrderItem orderItem = orderItemRepository
                .findById(orderItemId)
                .orElseThrow(() -> new IllegalStateException("Order item không tồn tại."));

        Order order = orderItem.getOrder();

        // 4️⃣ Check đúng chủ đơn
        if (order.getActor() == null
                || !order.getActor().getId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền đánh giá sản phẩm này.");
        }

        // 5️⃣ Check COMPLETED
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Chỉ có thể đánh giá khi đơn hàng đã hoàn tất.");
        }

        // 6️⃣ Lấy variant -> product
        ProductVariant variant = variantRepository
                .findById(orderItem.getVariantId())
                .orElseThrow(() -> new IllegalStateException("Variant không tồn tại."));

        Product product = variant.getProduct();

        User user = userRepository.findById(userId).orElseThrow();

        // 7️⃣ Tạo review
        Review review = new Review();
        review.setRating(rating);
        review.setComment(comment);
        review.setUser(user);
        review.setProduct(product);
        review.setOrderItem(orderItem);

        reviewRepository.save(review);

        // 8️⃣ Trả về orderId để redirect đúng
        return order.getId();
    }

    // ======================================================
    // RATING INFO
    // ======================================================
    public double getAverageRating(Long productId) {
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        return avg != null ? avg : 0.0;
    }

    public long getReviewCount(Long productId) {
        return reviewRepository.countByProductId(productId);
    }

    public List<Review> getReviewsByProduct(Long productId) {
        return reviewRepository.findAllByProductIdOrderByCreatedAtDesc(productId);
    }
}
