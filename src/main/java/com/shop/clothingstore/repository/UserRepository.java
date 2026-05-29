package com.shop.clothingstore.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface UserRepository extends BaseRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Referral: tìm user theo mã giới thiệu của họ
    Optional<User> findByReferralCode(String referralCode);

    boolean existsByReferralCode(String referralCode);

    /**
     * Đánh dấu "đã thưởng referral" một cách NGUYÊN TỬ (atomic check-and-set).
     * Chỉ cập nhật khi referral_rewarded đang false → trả về 1 nếu giành quyền thưởng,
     * 0 nếu đã được thưởng (tránh trao coupon trùng khi nhiều đơn hoàn tất đồng thời).
     */
    @Modifying
    @Query("UPDATE User u SET u.referralRewarded = true WHERE u.id = :id AND u.referralRewarded = false")
    int markReferralRewarded(@Param("id") Long id);

    // Admin paginated list (all users)
    Page<User> findAllByOrderByIdDesc(Pageable pageable);

    // Admin search: email, fullName, phone — combined with optional role filter
    @Query("""
        SELECT u FROM User u
        WHERE (:keyword IS NULL OR :keyword = ''
               OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR u.phone           LIKE CONCAT('%', :keyword, '%'))
          AND (:role IS NULL OR u.role = :role)
        ORDER BY u.id DESC
    """)
    Page<User> searchAdmin(
            @Param("keyword") String keyword,
            @Param("role")    Role role,
            Pageable pageable
    );
}