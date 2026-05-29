package com.shop.clothingstore.service;

import java.math.BigDecimal;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.UserRepository;

/**
 * Hệ thống mã giới thiệu (referral).
 *
 * <p>Luồng:</p>
 * <ol>
 *   <li>Mỗi user được cấp <code>referralCode</code> khi đăng ký (hoặc backfill cho user cũ).</li>
 *   <li>Người mới đăng ký kèm <code>?ref=CODE</code> sẽ được gắn <code>referredById</code>.</li>
 *   <li>Khi đơn ĐẦU TIÊN của người được giới thiệu hoàn tất (COMPLETED):
 *       cả người giới thiệu và người được giới thiệu đều nhận 1 coupon giảm giá.</li>
 * </ol>
 */
@Service
public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // bỏ ký tự dễ nhầm
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Phần thưởng referral
    private static final BigDecimal REWARD_AMOUNT = new BigDecimal("50000");
    private static final BigDecimal REWARD_MIN_ORDER = new BigDecimal("200000");
    private static final int REWARD_VALIDITY_DAYS = 30;

    private final UserRepository userRepository;
    private final CouponService couponService;

    public ReferralService(UserRepository userRepository, CouponService couponService) {
        this.userRepository = userRepository;
        this.couponService = couponService;
    }

    // ─────────────────────────────────────────────────────────────
    // SINH MÃ
    // ─────────────────────────────────────────────────────────────
    /** Đảm bảo user có referralCode; sinh mới nếu chưa có. KHÔNG tự lưu (caller lưu). */
    public void ensureReferralCode(User user) {
        if (user.getReferralCode() == null || user.getReferralCode().isBlank()) {
            user.setReferralCode(generateUniqueCode());
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = randomCode();
            if (!userRepository.existsByReferralCode(code)) {
                return code;
            }
        }
        // Cực hiếm: thêm hậu tố thời điểm để chắc chắn duy nhất
        return randomCode() + Long.toString(System.nanoTime(), 36).toUpperCase();
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // GẮN NGƯỜI GIỚI THIỆU KHI ĐĂNG KÝ
    // ─────────────────────────────────────────────────────────────
    /**
     * Gắn referredById cho user mới dựa trên mã giới thiệu (nếu hợp lệ).
     * Bỏ qua nếu mã rỗng, không tồn tại, hoặc tự giới thiệu chính mình.
     * KHÔNG tự lưu — caller chịu trách nhiệm persist newUser.
     */
    public void applyReferralCode(User newUser, String refCode) {
        if (refCode == null || refCode.isBlank()) {
            return;
        }
        String normalized = refCode.trim().toUpperCase();
        userRepository.findByReferralCode(normalized).ifPresent(referrer -> {
            if (newUser.getId() == null || !referrer.getId().equals(newUser.getId())) {
                newUser.setReferredById(referrer.getId());
                log.info("Referral linked | newUser={} | referrerId={}", newUser.getEmail(), referrer.getId());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // TRAO THƯỞNG KHI ĐƠN ĐẦU HOÀN TẤT
    // ─────────────────────────────────────────────────────────────
    /**
     * Gọi khi một đơn của buyer chuyển sang COMPLETED. Nếu buyer được ai đó giới thiệu
     * và chưa từng được thưởng → tặng coupon cho cả hai, đánh dấu đã thưởng.
     * Best-effort: lỗi ở đây KHÔNG được làm hỏng việc cập nhật đơn hàng (caller nên bọc try/catch).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rewardOnFirstCompletedOrder(User buyer) {
        if (buyer == null || buyer.getReferredById() == null || buyer.isReferralRewarded()) {
            return;
        }

        // Giành quyền thưởng một cách NGUYÊN TỬ — chỉ 1 luồng thắng, tránh coupon trùng
        // khi khách có nhiều đơn cùng hoàn tất đồng thời.
        if (userRepository.markReferralRewarded(buyer.getId()) == 0) {
            return; // đã được thưởng (hoặc luồng khác vừa giành)
        }

        User referrer = userRepository.findById(buyer.getReferredById()).orElse(null);
        if (referrer == null) {
            return;
        }

        // Tặng coupon cho người được giới thiệu (buyer)
        couponService.grantFixedCoupon(buyer, "REF", REWARD_AMOUNT, REWARD_MIN_ORDER,
                REWARD_VALIDITY_DAYS, "Cảm ơn bạn đã mua hàng qua giới thiệu! Giảm 50.000₫ cho đơn tiếp theo.");

        // Tặng coupon cho người giới thiệu (referrer)
        couponService.grantFixedCoupon(referrer, "REF", REWARD_AMOUNT, REWARD_MIN_ORDER,
                REWARD_VALIDITY_DAYS, "Bạn bè bạn giới thiệu đã mua hàng! Giảm 50.000₫ cho đơn tiếp theo.");

        log.info("Referral reward granted | buyerId={} | referrerId={}", buyer.getId(), referrer.getId());
    }
}
