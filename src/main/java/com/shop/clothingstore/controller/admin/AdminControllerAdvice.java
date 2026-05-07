package com.shop.clothingstore.controller.admin;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.repository.OrderRepository;

/**
 * Injects model attributes available to every admin template.
 *
 * pendingCount — number of PENDING orders, shown as badge on the sidebar Orders link.
 *               Computed once per request for all admin pages, not per controller.
 */
@ControllerAdvice(basePackages = "com.shop.clothingstore.controller.admin")
public class AdminControllerAdvice {

    private final OrderRepository orderRepository;

    public AdminControllerAdvice(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @ModelAttribute
    public void adminGlobal(Model model) {
        try {
            long pending = orderRepository.countByStatus(OrderStatus.PENDING);
            model.addAttribute("pendingCount", pending);
        } catch (Exception ignored) {
            model.addAttribute("pendingCount", 0L);
        }
    }
}
