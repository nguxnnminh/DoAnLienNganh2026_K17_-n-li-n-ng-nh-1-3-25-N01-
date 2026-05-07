package com.shop.clothingstore.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.Payment;
import com.shop.clothingstore.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public static final String METHOD_COD           = "COD";
    public static final String METHOD_BANK_TRANSFER = "BANK_TRANSFER";

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_PAID     = "PAID";
    public static final String STATUS_REFUNDED = "REFUNDED";

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Creates a Payment record when an order is placed.
     * Defaults to COD (Cash on Delivery) with PENDING status.
     * Idempotent — returns existing record if already created.
     */
    @Transactional
    public Payment createForOrder(Order order, String paymentMethod) {
        Optional<Payment> existing = paymentRepository.findByOrder(order);
        if (existing.isPresent()) {
            return existing.get();
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : METHOD_COD);
        payment.setStatus(STATUS_PENDING);
        payment.setAmount(order.getTotal() != null ? order.getTotal().doubleValue() : 0.0);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment record created | orderId={} | method={} | amount={}",
                order.getId(), saved.getPaymentMethod(), saved.getAmount());
        return saved;
    }

    /**
     * Marks a payment as received (PAID).
     * Called when order status moves to COMPLETED.
     */
    @Transactional
    public void markPaid(Order order) {
        paymentRepository.findByOrder(order).ifPresent(p -> {
            if (STATUS_PAID.equals(p.getStatus())) return;
            p.setStatus(STATUS_PAID);
            p.setPaidAt(LocalDateTime.now());
            paymentRepository.save(p);
            log.info("Payment marked as PAID | orderId={} | amount={}", order.getId(), p.getAmount());
        });
    }

    /**
     * Marks a payment as refunded.
     * Called when order is CANCELLED and payment was already PAID.
     */
    @Transactional
    public void markRefunded(Order order) {
        paymentRepository.findByOrder(order).ifPresent(p -> {
            if (STATUS_PAID.equals(p.getStatus())) {
                p.setStatus(STATUS_REFUNDED);
                paymentRepository.save(p);
                log.info("Payment marked as REFUNDED | orderId={}", order.getId());
            }
        });
    }

    public Optional<Payment> findByOrder(Order order) {
        return paymentRepository.findByOrder(order);
    }
}
