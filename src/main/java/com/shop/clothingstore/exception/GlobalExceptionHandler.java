package com.shop.clothingstore.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =====================================================
    // 404 - Resource Not Found
    // =====================================================
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.warn("Resource not found: {} | URI: {}", ex.getMessage(), request.getRequestURI());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return redirectToReferer(request);
    }

    // =====================================================
    // ResponseStatusException (404, 400, etc from controllers)
    // =====================================================
    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.warn("ResponseStatusException: {} | URI: {}", ex.getReason(), request.getRequestURI());
        redirectAttributes.addFlashAttribute("error",
                ex.getReason() != null ? ex.getReason() : "Không tìm thấy trang yêu cầu");
        return "redirect:/products";
    }

    // =====================================================
    // 404 - Spring static resource not found (css, js, images)
    // =====================================================
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        log.warn("Static resource not found: {}", request.getRequestURI());
        return "redirect:/";
    }

    // =====================================================
    // OUT OF STOCK
    // =====================================================
    @ExceptionHandler(OutOfStockException.class)
    public String handleOutOfStock(
            OutOfStockException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.warn("Out of stock: {} | Stock: {}", ex.getProductName(), ex.getAvailableStock());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return redirectToReferer(request);
    }

    // =====================================================
    // ILLEGAL STATE (cart empty, invalid operation, etc.)
    // =====================================================
    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.warn("Illegal state: {} | URI: {}", ex.getMessage(), request.getRequestURI());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return redirectToReferer(request);
    }

    // =====================================================
    // ILLEGAL ARGUMENT (bad input)
    // =====================================================
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.warn("Invalid input: {} | URI: {}", ex.getMessage(), request.getRequestURI());
        redirectAttributes.addFlashAttribute("error", "Dữ liệu không hợp lệ: " + ex.getMessage());
        return redirectToReferer(request);
    }

    // =====================================================
    // CATCH-ALL (unexpected errors)
    // =====================================================
    @ExceptionHandler(Exception.class)
    public String handleGeneral(
            Exception ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.error("Unexpected error at URI: {} | Type: {} | Message: {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex);
        redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
        return "redirect:/";
    }

    // =====================================================
    // HELPER: redirect về trang trước đó
    // =====================================================
    private String redirectToReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin")) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/";
    }
}
