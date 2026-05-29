package com.shop.clothingstore.entity;

import java.util.ArrayList;
import java.util.List;

import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString(exclude = {"password", "reviews"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;
    private String phone;
    private String address;

    @Enumerated(EnumType.STRING)
    private Role role;

    // ==============================
    // REFERRAL (mã giới thiệu)
    // referralCode : mã riêng của user này để giới thiệu người khác (unique)
    // referredById : id của user đã giới thiệu user này (nullable)
    // referralRewarded : đã trao thưởng cho cặp giới thiệu khi đơn đầu hoàn tất chưa
    // ==============================
    @Column(name = "referral_code", unique = true, length = 16)
    private String referralCode;

    @Column(name = "referred_by_id")
    private Long referredById;

    @Column(name = "referral_rewarded", nullable = false)
    private boolean referralRewarded = false;

    @OneToMany(mappedBy = "actor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();
}
