package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.dto.CartItemDTO;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.repository.ProductVariantRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class CartService {

    private static final String CART_SESSION_KEY = "CART";

    private final ProductVariantRepository variantRepository;
    private final HttpSession session;

    public CartService(ProductVariantRepository variantRepository,
            HttpSession session) {

        this.variantRepository = variantRepository;
        this.session = session;
    }

    // =====================================================
    // GET CART
    // =====================================================
    @SuppressWarnings("unchecked")
    public List<CartItemDTO> getCart() {

        List<CartItemDTO> cart
                = (List<CartItemDTO>) session.getAttribute(CART_SESSION_KEY);

        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }

        return cart;
    }

    // =====================================================
    // ADD TO CART
    // =====================================================
    public void addToCart(Long variantId, int quantity) {

        if (variantId == null || quantity < 1) {
            return;
        }

        ProductVariant variant = variantRepository
                .findById(variantId)
                .orElse(null);

        if (variant == null) {
            return;
        }

        int stock = variant.getStock();

        // Hết hàng
        if (stock <= 0) {
            return;
        }

        List<CartItemDTO> cart = getCart();

        for (CartItemDTO item : cart) {

            if (item.getVariantId().equals(variantId)) {

                int newQty = item.getQuantity() + quantity;

                // Không cho vượt stock
                if (newQty > stock) {
                    newQty = stock;
                }

                item.setQuantity(newQty);
                return;
            }
        }

        // Nếu là item mới
        int finalQty = Math.min(quantity, stock);

        CartItemDTO newItem = new CartItemDTO();

        newItem.setVariantId(variantId);
        newItem.setProductName(variant.getProduct().getName());

        newItem.setImageUrl(
                variant.getProduct().getImages().isEmpty()
                ? ""
                : variant.getProduct().getImages()
                        .stream()
                        .findFirst()
                        .map(img -> img.getImageUrl())
                        .orElse("")
        );

        newItem.setSize(variant.getSize());
        newItem.setColor(variant.getColor());
        newItem.setPrice(BigDecimal.valueOf(variant.getPrice()));
        newItem.setQuantity(finalQty);

        cart.add(newItem);
    }

    // =====================================================
    // UPDATE QUANTITY
    // =====================================================
    public void updateQuantity(Long variantId, int quantity) {

        if (variantId == null || quantity < 1) {
            return;
        }

        ProductVariant variant = variantRepository
                .findById(variantId)
                .orElse(null);

        if (variant == null) {
            return;
        }

        // Không cho vượt stock
        if (quantity > variant.getStock()) {
            quantity = variant.getStock();
        }

        for (CartItemDTO item : getCart()) {

            if (item.getVariantId().equals(variantId)) {
                item.setQuantity(quantity);
                return;
            }
        }
    }

    // =====================================================
    // REMOVE ITEM
    // =====================================================
    public void remove(Long variantId) {

        getCart().removeIf(item
                -> item.getVariantId().equals(variantId)
        );
    }

    // =====================================================
    // CLEAR CART
    // =====================================================
    public void clear() {
        session.removeAttribute(CART_SESSION_KEY);
    }

    // =====================================================
    // TOTAL PRICE
    // =====================================================
    public BigDecimal getTotal() {

        return getCart().stream()
                .map(item
                        -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
