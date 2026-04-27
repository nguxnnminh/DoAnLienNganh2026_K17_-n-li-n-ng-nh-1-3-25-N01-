package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.dto.CartItemDTO;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.repository.ProductVariantRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class CartService {

    private static final String CART_SESSION_KEY = "CART";

    private final ProductVariantRepository variantRepository;
    private final HttpServletRequest request;

    public CartService(ProductVariantRepository variantRepository,
            HttpServletRequest request) {
        this.variantRepository = variantRepository;
        this.request = request;
    }

    // =====================================================
    // HELPER: always get fresh session from current request
    // =====================================================
    private HttpSession getSession() {
        return request.getSession(true);
    }

    // =====================================================
    // GET CART
    // =====================================================
    @SuppressWarnings("unchecked")
    public List<CartItemDTO> getCart() {
        HttpSession session = getSession();
        List<CartItemDTO> cart = (List<CartItemDTO>) session.getAttribute(CART_SESSION_KEY);
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
            throw new IllegalStateException("Dữ liệu không hợp lệ");
        }

        ProductVariant variant = variantRepository
                .findById(variantId)
                .orElseThrow(() -> new IllegalStateException("Sản phẩm không tồn tại"));

        if (!variant.getProduct().isActive()) {
            throw new IllegalStateException(
                    "Sản phẩm " + variant.getProduct().getName() + " hiện không còn bán");
        }

        int stock = variant.getStock();
        if (stock <= 0) {
            throw new IllegalStateException(
                    "Sản phẩm " + variant.getProduct().getName() + " đã hết hàng");
        }

        HttpSession session = getSession();

        @SuppressWarnings("unchecked")
        List<CartItemDTO> cart = (List<CartItemDTO>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
        }

        for (CartItemDTO item : cart) {
            if (item.getVariantId().equals(variantId)) {
                int newQty = item.getQuantity() + quantity;
                if (newQty > stock) {
                    newQty = stock;
                }
                item.setQuantity(newQty);
                session.setAttribute(CART_SESSION_KEY, cart);
                return;
            }
        }

        int finalQty = Math.min(quantity, stock);
        CartItemDTO newItem = new CartItemDTO();
        newItem.setVariantId(variantId);
        newItem.setProductName(variant.getProduct().getName());
        newItem.setImageUrl(
                variant.getProduct().getImages().isEmpty()
                ? ""
                : variant.getProduct().getImages().stream()
                        .findFirst()
                        .map(img -> img.getImageUrl())
                        .orElse(""));
        newItem.setSize(variant.getSize());
        newItem.setColor(variant.getColor());
        newItem.setPrice(BigDecimal.valueOf(variant.getPrice()));
        newItem.setQuantity(finalQty);

        cart.add(newItem);
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    // =====================================================
    // UPDATE QUANTITY
    // =====================================================
    public void updateQuantity(Long variantId, int quantity) {
        if (variantId == null || quantity < 1) {
            throw new IllegalStateException("Dữ liệu không hợp lệ");
        }

        ProductVariant variant = variantRepository
                .findById(variantId)
                .orElseThrow(() -> new IllegalStateException("Sản phẩm không tồn tại"));

        if (!variant.getProduct().isActive()) {
            throw new IllegalStateException(
                    "Sản phẩm " + variant.getProduct().getName() + " hiện không còn bán");
        }

        if (quantity > variant.getStock()) {
            quantity = variant.getStock();
        }

        HttpSession session = getSession();

        @SuppressWarnings("unchecked")
        List<CartItemDTO> cart = (List<CartItemDTO>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            return;
        }

        for (CartItemDTO item : cart) {
            if (item.getVariantId().equals(variantId)) {
                item.setQuantity(quantity);
                session.setAttribute(CART_SESSION_KEY, cart);
                return;
            }
        }
    }

    // =====================================================
    // REMOVE ITEM
    // =====================================================
    public void remove(Long variantId) {
        HttpSession session = getSession();

        @SuppressWarnings("unchecked")
        List<CartItemDTO> cart = (List<CartItemDTO>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            return;
        }

        cart.removeIf(item -> item.getVariantId().equals(variantId));
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    // =====================================================
    // CLEAR CART
    // =====================================================
    public void clear() {
        getSession().removeAttribute(CART_SESSION_KEY);
    }

    // =====================================================
    // TOTAL PRICE
    // =====================================================
    public BigDecimal getTotal() {
        return getCart().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
