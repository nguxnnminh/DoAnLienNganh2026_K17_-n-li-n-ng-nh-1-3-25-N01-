package com.shop.clothingstore.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Quản lý kết nối Server-Sent Events (SSE) để đẩy thông báo real-time tới trình duyệt.
 *
 * <p>Giữ registry các {@link SseEmitter} theo userId (mỗi user có thể mở nhiều tab) và
 * một danh sách riêng cho admin (nhận thông báo đơn mới). Tất cả thao tác push đều
 * "best-effort": nếu một emitter chết, nó tự bị loại bỏ và KHÔNG làm ảnh hưởng nghiệp vụ.</p>
 *
 * <p>Endpoint nằm ở web-chain (session-auth) chứ không phải /api/ (stateless) nên
 * trình duyệt dùng EventSource kèm cookie phiên là kết nối được.</p>
 */
@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    // 30 phút — sau đó trình duyệt EventSource tự kết nối lại.
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final List<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();

    // ─────────────────────────────────────────────────────────────
    // SUBSCRIBE
    // ─────────────────────────────────────────────────────────────
    public SseEmitter subscribe(Long userId, boolean isAdmin) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        // Admin nhận sự kiện qua kênh admin riêng; KHÔNG đăng ký thêm vào kênh user
        // để tránh nhận trùng thông báo (vd: đơn mới đẩy cả 2 kênh).
        if (isAdmin) {
            adminEmitters.add(emitter);
        } else if (userId != null) {
            userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        }

        Long registeredUserId = isAdmin ? null : userId;
        Runnable cleanup = () -> remove(registeredUserId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // Gửi 1 event "connected" để client biết stream đã mở (và mở khóa onopen)
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(registeredUserId, emitter);
        }
        return emitter;
    }

    private void remove(Long userId, SseEmitter emitter) {
        if (userId != null) {
            // compute() để check-and-remove nguyên tử, tránh xóa nhầm list vừa được tạo lại
            userEmitters.compute(userId, (k, list) -> {
                if (list == null) {
                    return null;
                }
                list.remove(emitter);
                return list.isEmpty() ? null : list;
            });
        }
        adminEmitters.remove(emitter);
    }

    // ─────────────────────────────────────────────────────────────
    // PUSH
    // ─────────────────────────────────────────────────────────────
    public void pushToUser(Long userId, String eventName, Object data) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> list = userEmitters.get(userId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            sendQuietly(emitter, eventName, data, () -> remove(userId, emitter));
        }
    }

    public void pushToAdmins(String eventName, Object data) {
        for (SseEmitter emitter : adminEmitters) {
            sendQuietly(emitter, eventName, data, () -> adminEmitters.remove(emitter));
        }
    }

    private void sendQuietly(SseEmitter emitter, String eventName, Object data, Runnable onFail) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            log.debug("SSE send failed, removing emitter: {}", e.getMessage());
            onFail.run();
        }
    }
}
