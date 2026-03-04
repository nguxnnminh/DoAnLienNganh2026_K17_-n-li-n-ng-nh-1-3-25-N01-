package com.shop.clothingstore.entity.base;

import java.time.LocalDateTime;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

@MappedSuperclass
public abstract class AbstractTransaction<
        S extends Enum<S>, // Status type
        U extends BaseEntity // Actor type (User, Student, Instructor,...)
        > extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "actor_id")
    protected U actor;

    @Enumerated(EnumType.STRING)
    protected S status;

    protected LocalDateTime transactionDate;

    @PrePersist
    protected void onTransactionCreate() {
        this.transactionDate = LocalDateTime.now();
    }

    // ===== Getter & Setter =====
    public U getActor() {
        return actor;
    }

    public void setActor(U actor) {
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
}
