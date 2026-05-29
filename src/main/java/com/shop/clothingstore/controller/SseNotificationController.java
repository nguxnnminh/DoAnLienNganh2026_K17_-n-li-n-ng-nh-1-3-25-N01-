package com.shop.clothingstore.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.SseService;
import com.shop.clothingstore.service.UserService;

/**
 * Endpoint SSE cho thông báo real-time.
 *
 * <p>Đặt ngoài /api/ (dưới web security chain) nên dùng xác thực bằng session-cookie —
 * trình duyệt mở <code>new EventSource('/notifications/stream')</code> là tự gửi cookie.</p>
 */
@RestController
public class SseNotificationController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final SseService sseService;
    private final UserService userService;

    public SseNotificationController(SseService sseService, UserService userService) {
        this.sseService = sseService;
        this.userService = userService;
    }

    @GetMapping(path = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // Web chain đã yêu cầu auth; phòng thủ thêm — trả emitter rỗng hoàn tất ngay
            SseEmitter empty = new SseEmitter(0L);
            empty.complete();
            return empty;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));

        Long userId = userService.findByEmail(authentication.getName())
                .map(User::getId)
                .orElse(null);

        return sseService.subscribe(userId, isAdmin);
    }
}
