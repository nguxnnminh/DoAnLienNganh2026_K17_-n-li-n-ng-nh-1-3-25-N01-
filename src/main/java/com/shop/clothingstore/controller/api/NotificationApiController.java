package com.shop.clothingstore.controller.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.entity.Notification;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.UserRepository;
import com.shop.clothingstore.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationApiController(NotificationService notificationService,
                                     UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /** Unread badge count — polled by the header bell icon. */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count(
            @AuthenticationPrincipal UserDetails principal) {

        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.ok(Map.of("count", 0L));
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(user)));
    }

    /** Recent 20 notifications for the dropdown panel. */
    @GetMapping
    public ResponseEntity<List<Notification>> list(
            @AuthenticationPrincipal UserDetails principal) {

        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(notificationService.getRecent(user, 20));
    }

    /** Mark a single notification as read. */
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Boolean>> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).body(Map.of("success", false));
        boolean done = notificationService.markAsRead(id, user);
        return ResponseEntity.ok(Map.of("success", done));
    }

    /** Mark all notifications as read. */
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Boolean>> markAllRead(
            @AuthenticationPrincipal UserDetails principal) {

        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).body(Map.of("success", false));
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private User resolveUser(UserDetails principal) {
        if (principal == null) return null;
        return userRepository.findByEmail(principal.getUsername()).orElse(null);
    }
}
