package com.shop.clothingstore.entity;

import java.util.ArrayList;
import java.util.List;

import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"order_item_id"}
        )
)
public class Review extends BaseEntity {

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private double rating;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String comment;

    // ==============================
    // ẢNH ĐÍNH KÈM (tùy chọn) — lưu URL ảnh do khách upload khi đánh giá
    // Bảng phụ review_images(review_id, image_url). Tối đa vài ảnh / review.
    // ==============================
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url", length = 512)
    private List<String> imageUrls = new ArrayList<>();

    // ==============================
    // ACTOR (USER)
    // ==============================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    // ==============================
    // ITEM ID (GENERIC)
    // ==============================
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    // ==============================
    // ORDER ITEM (CORE RELATION)
    // ==============================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // ==============================
    // GETTER / SETTER
    // ==============================
    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }
}
