package com.shop.clothingstore.controller;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.OrderService;
import com.shop.clothingstore.service.ReviewService;
import com.shop.clothingstore.service.UserService;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final ReviewService reviewService;

    public OrderController(
            OrderService orderService,
            UserService userService,
            ReviewService reviewService
    ) {
        this.orderService = orderService;
        this.userService = userService;
        this.reviewService = reviewService;
    }

    // ===============================
    // CANCEL ORDER (PENDING only)
    // ===============================
    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            User user = userService.findByEmail(principal.getName()).orElseThrow();
            orderService.selfCancel(id, user);
            redirectAttributes.addFlashAttribute("success", "Order cancelled successfully.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not cancel order.");
        }
        return "redirect:/orders/" + id;
    }

    // ===============================
    // REQUEST CANCELLATION (PROCESSING only)
    // ===============================
    @PostMapping("/orders/{id}/cancel-request")
    public String requestCancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String reason,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            User user = userService.findByEmail(principal.getName()).orElseThrow();
            orderService.requestCancel(id, user, reason);
            redirectAttributes.addFlashAttribute("success", "Cancellation request submitted.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not submit cancellation request.");
        }
        return "redirect:/orders/" + id;
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

        User user = userService
                .findByEmail(principal.getName())
                .orElseThrow();

        List<Order> orders = orderService.findOrdersByUser(user);

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

        try {

            User user = userService
                    .findByEmail(principal.getName())
                    .orElseThrow();

            Order order = orderService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Only allow viewing own orders
            if (order.getActor() == null
                    || !order.getActor().getId().equals(user.getId())) {
                return "redirect:/my-orders";
            }

            boolean isCompleted
                    = order.getStatus() == OrderStatus.COMPLETED;

            Set<Long> reviewedItemIds = order.getItems()
                    .stream()
                    .filter(item
                            -> reviewService.hasReviewByOrderItem(item.getId())
                    )
                    .map(item -> item.getId())
                    .collect(Collectors.toSet());

            model.addAttribute("order", order);
            model.addAttribute("isCompleted", isCompleted);
            model.addAttribute("reviewedItemIds", reviewedItemIds);

            return "shop/order-detail";

        } catch (Exception e) {

            return "redirect:/my-orders";
        }
    }
}
