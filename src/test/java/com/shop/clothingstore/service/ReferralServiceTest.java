package com.shop.clothingstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.UserRepository;

/**
 * Unit test cho ReferralService — hệ thống mã giới thiệu (F5).
 * Kiểm tra: sinh mã, gắn người giới thiệu, chống tự giới thiệu, và trao thưởng
 * NGUYÊN TỬ (không trùng) khi đơn đầu hoàn tất.
 */
@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private ReferralService referralService;

    private User newUser() {
        User u = new User();
        u.setEmail("u@example.com");
        return u;
    }

    private void setId(User u, Long id) {
        ReflectionTestUtils.setField(u, "id", id);
    }

    // ── ensureReferralCode ─────────────────────────────────────────
    @Test
    void ensureReferralCode_generatesWhenMissing() {
        when(userRepository.existsByReferralCode(anyString())).thenReturn(false);
        User u = newUser();

        referralService.ensureReferralCode(u);

        assertThat(u.getReferralCode()).isNotBlank();
        assertThat(u.getReferralCode()).hasSize(8);
    }

    @Test
    void ensureReferralCode_keepsExisting() {
        User u = newUser();
        u.setReferralCode("EXISTING1");

        referralService.ensureReferralCode(u);

        assertThat(u.getReferralCode()).isEqualTo("EXISTING1");
    }

    // ── applyReferralCode ──────────────────────────────────────────
    @Test
    void applyReferralCode_linksValidReferrer() {
        User referrer = newUser();
        setId(referrer, 1L);
        when(userRepository.findByReferralCode("ABC123XY")).thenReturn(Optional.of(referrer));

        User newbie = newUser();
        referralService.applyReferralCode(newbie, "abc123xy"); // lowercase → normalized

        assertThat(newbie.getReferredById()).isEqualTo(1L);
    }

    @Test
    void applyReferralCode_ignoresBlankOrUnknown() {
        User newbie = newUser();
        referralService.applyReferralCode(newbie, "  ");
        assertThat(newbie.getReferredById()).isNull();

        when(userRepository.findByReferralCode("NOPE")).thenReturn(Optional.empty());
        referralService.applyReferralCode(newbie, "NOPE");
        assertThat(newbie.getReferredById()).isNull();
    }

    @Test
    void applyReferralCode_preventsSelfReferral() {
        User self = newUser();
        setId(self, 5L);
        when(userRepository.findByReferralCode("SELF0001")).thenReturn(Optional.of(self));

        referralService.applyReferralCode(self, "SELF0001");

        assertThat(self.getReferredById()).isNull();
    }

    // ── rewardOnFirstCompletedOrder ────────────────────────────────
    @Test
    void reward_grantsCouponsToBothWhenClaimed() {
        User buyer = newUser();
        setId(buyer, 10L);
        buyer.setReferredById(1L);

        User referrer = newUser();
        setId(referrer, 1L);

        // markReferralRewarded trả 1 → giành quyền thưởng
        when(userRepository.markReferralRewarded(10L)).thenReturn(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(referrer));

        referralService.rewardOnFirstCompletedOrder(buyer);

        // Cả buyer và referrer đều nhận 1 coupon
        verify(couponService, times(2)).grantFixedCoupon(
                any(User.class), eq("REF"), any(BigDecimal.class), any(BigDecimal.class), anyInt(), anyString());
    }

    @Test
    void reward_skipsWhenAlreadyClaimed() {
        User buyer = newUser();
        setId(buyer, 10L);
        buyer.setReferredById(1L);

        // markReferralRewarded trả 0 → đã thưởng / luồng khác giành → KHÔNG trao lại
        when(userRepository.markReferralRewarded(10L)).thenReturn(0);

        referralService.rewardOnFirstCompletedOrder(buyer);

        verify(couponService, never()).grantFixedCoupon(
                any(), anyString(), any(), any(), anyInt(), anyString());
    }

    @Test
    void reward_skipsWhenNoReferrer() {
        User buyer = newUser();
        setId(buyer, 10L);
        buyer.setReferredById(null); // không ai giới thiệu

        referralService.rewardOnFirstCompletedOrder(buyer);

        verify(userRepository, never()).markReferralRewarded(any());
        verify(couponService, never()).grantFixedCoupon(
                any(), anyString(), any(), any(), anyInt(), anyString());
    }
}
