package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.dto.CartItem;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.repository.ProductVariantRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class CartService {

    private static final String CART_SESSION_KEY = "CART";

    private final ProductVariantRepository variantRepository;
    private final HttpSession session;

    public CartService(ProductVariantRepository variantRepository, HttpSession session) {
        this.variantRepository = variantRepository;
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    public List<CartItem> getCart() {
        var cart = (List<CartItem>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    public void addToCart(Long variantId) {
        if (variantId == null) return;

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow();

        List<CartItem> cart = getCart();

        for (CartItem item : cart) {
            if (item.getVariantId().equals(variantId)) {
                item.setQuantity(item.getQuantity() + 1);
                return;
            }
        }

        CartItem item = new CartItem();
        item.setVariantId(variantId);
        item.setProductName(variant.getProduct().getName());
        item.setImageUrl(
                variant.getProduct().getImages().isEmpty()
                        ? ""
                        : variant.getProduct().getImages().get(0).getImageUrl()
        );
        item.setSize(variant.getSize());
        item.setColor(variant.getColor());
        item.setPrice(BigDecimal.valueOf(variant.getPrice()));
        item.setQuantity(1);

        cart.add(item);
    }

    public void updateQuantity(Long variantId, int quantity) {
        if (variantId == null || quantity < 1) return;

        for (CartItem item : getCart()) {
            if (item.getVariantId().equals(variantId)) {
                item.setQuantity(quantity);
                return;
            }
        }
    }

    public void remove(Long variantId) {
        getCart().removeIf(item -> item.getVariantId().equals(variantId));
    }

    public void clear() {
        session.removeAttribute(CART_SESSION_KEY);
    }

    public BigDecimal getTotal() {
        return getCart().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
