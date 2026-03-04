package com.shop.clothingstore.controller.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.service.OrderService;

@Controller
public class AdminOrderController extends AdminBaseController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ===============================
    // ADMIN - ORDER LIST
    // ===============================
    @GetMapping("/admin/orders")
    public String orders(Model model) {

        model.addAttribute("title", "Quản lý đơn hàng");

        List<Order> orders = orderService.getAllOrders();

        model.addAttribute("orders", orders);
        model.addAttribute("statuses", OrderStatus.values());

        return "admin/orders/index";
    }

    // ===============================
    // ADMIN - ORDER DETAIL
    // ===============================
    @GetMapping("/admin/orders/{id}")
    public String orderDetail(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            Order order = orderService.findById(id) // 🔥 dùng generic
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

            model.addAttribute("title", "Chi tiết đơn hàng #" + id);
            model.addAttribute("order", order);
            model.addAttribute("statuses", OrderStatus.values());

            return "admin/orders/show";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Không tìm thấy đơn hàng."
            );

            return "redirect:/admin/orders";
        }
    }

    // ===============================
    // ADMIN - UPDATE STATUS
    // ===============================
    @PostMapping("/admin/orders/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status,
            RedirectAttributes redirectAttributes) {

        try {

            orderService.updateOrderStatus(id, status);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Cập nhật trạng thái đơn hàng thành công!"
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Lỗi hệ thống. Vui lòng thử lại."
            );
        }

        return "redirect:/admin/orders/" + id;
    }
}
