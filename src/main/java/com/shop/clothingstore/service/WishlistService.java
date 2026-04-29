package com.shop.clothingstore.service;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.entity.WishlistItem;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.WishlistItemRepository;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
    }

    public List<WishlistItem> getWishlist(User user) {
        return wishlistItemRepository.findByUser(user);
    }

    public boolean isInWishlist(User user, Long productId) {
        return wishlistItemRepository.existsByUserAndProductId(user, productId);
    }

    @Transactional
    public void addToWishlist(User user, @NonNull Long productId) {
        Objects.requireNonNull(productId, "productId must not be null");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Sản phẩm không tồn tại"));

        if (wishlistItemRepository.existsByUserAndProduct(user, product)) {
            return;
        }

        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        wishlistItemRepository.save(item);
    }

    @Transactional
    public void removeFromWishlist(User user, @NonNull Long productId) {
        Objects.requireNonNull(productId, "productId must not be null");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Sản phẩm không tồn tại"));
        wishlistItemRepository.deleteByUserAndProduct(user, product);
    }
}
