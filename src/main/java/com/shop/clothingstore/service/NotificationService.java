package com.shop.clothingstore.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Notification;
import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.NotificationRepository;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SseService sseService;

    public NotificationService(NotificationRepository notificationRepository,
            SseService sseService) {
        this.notificationRepository = notificationRepository;
        this.sseService = sseService;
    }

    // ─────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────
    // REQUIRES_NEW trên các method public (gọi từ OrderService/CheckoutService — bean khác,
    // nên proxy AOP áp dụng): thông báo chạy trong transaction RIÊNG, lỗi lưu notification
    // KHÔNG làm rollback nghiệp vụ chính (đặt đơn / đổi trạng thái).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyOrderPlaced(User user, Order order) {
        create(user,
                "Order placed successfully",
                "Your order #" + order.getId() + " has been placed. We will process it as soon as possible.",
                "ORDER_PLACED",
                order.getId(), "Order");

        // Real-time: báo cho tất cả admin đang online có đơn mới
        sseService.pushToAdmins("new-order", Map.of(
                "title", "Đơn hàng mới #" + order.getId(),
                "message", "Khách " + (order.getCustomerName() != null ? order.getCustomerName() : "ẩn danh")
                        + " vừa đặt đơn #" + order.getId(),
                "orderId", order.getId()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyOrderStatusChanged(User user, Order order) {
        String statusLabel = switch (order.getStatus()) {
            case PROCESSING ->
                "is being processed";
            case SHIPPING ->
                "is being shipped";
            case COMPLETED ->
                "has been completed";
            case CANCELLED ->
                "has been cancelled";
            case CANCEL_REQUESTED ->
                "is awaiting cancellation review";
            default ->
                order.getStatus().name().toLowerCase();
        };

        create(user,
                "Order #" + order.getId() + " update",
                "Your order " + statusLabel + ".",
                "ORDER_STATUS",
                order.getId(), "Order");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyCancelRequested(User user, Order order) {
        create(user,
                "Cancellation request for order #" + order.getId() + " submitted",
                "We have received your cancellation request. The shop will review and respond shortly.",
                "CANCEL_REQUESTED",
                order.getId(), "Order");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyCancelAccepted(User user, Order order) {
        create(user,
                "Cancellation request for order #" + order.getId() + " accepted",
                "Your order has been cancelled. If you have paid, a refund will be processed shortly.",
                "CANCEL_ACCEPTED",
                order.getId(), "Order");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyCancelDenied(User user, Order order) {
        create(user,
                "Cancellation request for order #" + order.getId() + " rejected",
                "The shop has rejected the cancellation request. Your order will continue to be processed.",
                "CANCEL_DENIED",
                order.getId(), "Order");
    }

    // ─────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────
    public List<Notification> getRecent(User user, int limit) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().limit(limit).toList();
    }

    public List<Notification> getAll(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    // ─────────────────────────────────────────────────────────────────
    // MARK READ
    // ─────────────────────────────────────────────────────────────────
    @Transactional
    public boolean markAsRead(Long notificationId, User user) {
        if (notificationId == null) {
            return false;
        }
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(user.getId()))
                .map(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unread
                = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        log.debug("Marked {} notifications as read | user={}", unread.size(), user.getEmail());
    }

    // ─────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────
    private void create(User user, String title, String message,
            String type, Long referenceId, String referenceType) {
        try {
            Notification n = new Notification();
            n.setUser(user);
            n.setTitle(title);
            n.setMessage(message);
            n.setType(type);
            n.setReferenceId(referenceId);
            n.setReferenceType(referenceType);
            notificationRepository.save(n);
            log.debug("Notification created | user={} | type={} | ref={}", user.getEmail(), type, referenceId);

            // Real-time: đẩy ngay tới user đang online (nếu có kết nối SSE)
            sseService.pushToUser(user.getId(), "notification", Map.of(
                    "title", title,
                    "message", message,
                    "type", type
            ));
        } catch (Exception e) {
            // Notification failure must not roll back the business transaction
            log.warn("Failed to create notification | user={} | type={} | {}", user.getEmail(), type, e.getMessage());
        }
    }
}
