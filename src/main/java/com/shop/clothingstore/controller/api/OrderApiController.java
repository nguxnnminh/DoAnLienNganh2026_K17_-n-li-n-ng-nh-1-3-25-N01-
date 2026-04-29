package com.shop.clothingstore.controller.api;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.dto.api.CheckoutRequest;
import com.shop.clothingstore.dto.api.OrderResponse;
import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.CartService;
import com.shop.clothingstore.service.CheckoutService;
import com.shop.clothingstore.service.OrderService;
import com.shop.clothingstore.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;
    private final UserService userService;
    private final CartService cartService;

    public OrderApiController(
            CheckoutService checkoutService,
            OrderService orderService,
            UserService userService,
            CartService cartService) {
        this.checkoutService = checkoutService;
        this.orderService = orderService;
        this.userService = userService;
        this.cartService = cartService;
    }

    // =====================================================
    // POST /api/orders/checkout
    // FIX: truyền couponCode từ request vào CheckoutService
    // =====================================================
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            Principal principal) {

        User user = getUser(principal);
        Order order = checkoutService.checkout(
                request.getCustomerName(),
                request.getPhone(),
                request.getAddress(),
                cartService.getCart(),
                user,
                request.getCouponCode(),
                request.getNote()
        );
        cartService.clear();
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    // =====================================================
    // GET /api/orders/my
    // =====================================================
    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> myOrders(Principal principal) {
        User user = getUser(principal);
        if (user == null) {
            throw new IllegalStateException("Bạn cần đăng nhập để xem đơn hàng");
        }

        List<OrderResponse> orders = orderService.findOrdersByUser(user).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    private User getUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.findByEmail(principal.getName()).orElse(null);
    }
}
