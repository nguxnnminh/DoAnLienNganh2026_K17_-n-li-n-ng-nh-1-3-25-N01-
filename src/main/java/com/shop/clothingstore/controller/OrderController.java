package com.shop.clothingstore.controller;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ReviewRepository;
import com.shop.clothingstore.repository.UserRepository;

@Controller
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public OrderController(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    // ===============================
    // MY ORDERS (LIST)
    // ===============================
    @GetMapping("/my-orders")
    public String myOrders(
            Principal principal,
            Model model
    ) {

        if (principal == null) {
            return "redirect:/login";
        }

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow();

        // 🔥 SỬA Ở ĐÂY
        List<Order> orders = orderRepository
                .findByActorOrderByCreatedAtDesc(user);

        model.addAttribute("orders", orders);

        return "shop/my-orders";
    }

    // ===============================
    // ORDER DETAIL
    // ===============================
    @GetMapping("/orders/{id}")
    public String orderDetail(
            @PathVariable Long id,
            Principal principal,
            Model model
    ) {

        if (principal == null) {
            return "redirect:/login";
        }

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow();

        Order order = orderRepository
                .findById(id)
                .orElseThrow();

        // 🔐 Chỉ xem đơn của mình
        if (order.getActor() == null
                || !order.getActor().getId().equals(user.getId())) {
            return "redirect:/my-orders";
        }

        // ✅ Check COMPLETED
        boolean isCompleted = order.getStatus() == OrderStatus.COMPLETED;

        // ✅ Lấy danh sách orderItem đã review
        Set<Long> reviewedItemIds = order.getItems()
                .stream()
                .filter(item
                        -> reviewRepository.findByOrderItemId(item.getId()).isPresent()
                )
                .map(item -> item.getId())
                .collect(Collectors.toSet());

        model.addAttribute("order", order);
        model.addAttribute("isCompleted", isCompleted);
        model.addAttribute("reviewedItemIds", reviewedItemIds);

        return "shop/order-detail";
    }
}
