package com.shop.clothingstore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.UserRepository;
import com.shop.clothingstore.service.ReferralService;

/**
 * Gán referralCode cho các user CŨ chưa có mã (vd: dev users tạo bởi DataInitializer,
 * hoặc dữ liệu có sẵn khi người khác pull code về).
 *
 * <p>Chạy 1 lần lúc khởi động (ApplicationReadyEvent). Idempotent: chỉ xử lý user thiếu mã.</p>
 */
@Component
public class ReferralBackfillInitializer {

    private static final Logger log = LoggerFactory.getLogger(ReferralBackfillInitializer.class);

    private final UserRepository userRepository;
    private final ReferralService referralService;

    public ReferralBackfillInitializer(UserRepository userRepository, ReferralService referralService) {
        this.userRepository = userRepository;
        this.referralService = referralService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfill() {
        try {
            int updated = 0;
            for (User user : userRepository.findAll()) {
                if (user.getReferralCode() == null || user.getReferralCode().isBlank()) {
                    referralService.ensureReferralCode(user);
                    userRepository.save(user);
                    updated++;
                }
            }
            if (updated > 0) {
                log.info("Referral backfill: assigned codes to {} user(s)", updated);
            }
        } catch (Exception e) {
            log.warn("Referral backfill skipped: {}", e.getMessage());
        }
    }
}
