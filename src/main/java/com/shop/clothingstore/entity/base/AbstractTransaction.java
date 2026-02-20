package com.shop.clothingstore.entity.base;

import java.time.LocalDateTime;

import com.shop.clothingstore.entity.User;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

@MappedSuperclass
public abstract class AbstractTransaction<S extends Enum<S>> extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id")
    protected User actor;

    @Enumerated(EnumType.STRING)
    protected S status;

    protected LocalDateTime transactionDate;

    @PrePersist
    protected void onTransactionCreate() {
        this.transactionDate = LocalDateTime.now();
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public S getStatus() {
        return status;
    }

    public void setStatus(S status) {
        this.status = status;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    // 👇 QUAN TRỌNG
    @jakarta.persistence.Transient
    public User getUser() {
        return actor;
    }

    @jakarta.persistence.Transient
    public void setUser(User user) {
        this.actor = user;
    }
}