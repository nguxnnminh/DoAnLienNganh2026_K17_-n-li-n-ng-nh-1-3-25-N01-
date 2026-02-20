package com.shop.clothingstore.entity;

import java.time.LocalDateTime;

import com.shop.clothingstore.entity.base.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PasswordResetToken extends BaseEntity {

    private String token;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime expiryDate;
}