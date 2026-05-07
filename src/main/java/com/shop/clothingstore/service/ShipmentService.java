package com.shop.clothingstore.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.Shipment;
import com.shop.clothingstore.repository.ShipmentRepository;

@Service
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    // Status values stored as String in DB
    public static final String STATUS_PREPARING = "PREPARING";
    public static final String STATUS_SHIPPED   = "SHIPPED";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_RETURNED  = "RETURNED";

    private final ShipmentRepository shipmentRepository;

    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    /**
     * Called when order moves to PROCESSING.
     * Creates a new Shipment record in PREPARING status.
     */
    @Transactional
    public Shipment createForOrder(Order order) {
        // Idempotent — skip if one already exists
        Optional<Shipment> existing = shipmentRepository.findByOrder(order);
        if (existing.isPresent()) {
            return existing.get();
        }

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(STATUS_PREPARING);
        shipment.setShippingAddress(order.getAddress());
        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipment created | orderId={} | status={}", order.getId(), STATUS_PREPARING);
        return saved;
    }

    /**
     * Syncs shipment status to match the new order status.
     * Called every time order status changes.
     */
    @Transactional
    public void syncWithOrderStatus(Order order, OrderStatus newStatus) {
        Optional<Shipment> opt = shipmentRepository.findByOrder(order);

        switch (newStatus) {
            case PROCESSING -> createForOrder(order);

            case SHIPPING -> opt.ifPresent(s -> {
                s.setStatus(STATUS_SHIPPED);
                s.setShippedAt(LocalDateTime.now());
                shipmentRepository.save(s);
                log.info("Shipment shipped | orderId={}", order.getId());
            });

            case COMPLETED -> opt.ifPresent(s -> {
                s.setStatus(STATUS_DELIVERED);
                s.setDeliveredAt(LocalDateTime.now());
                shipmentRepository.save(s);
                log.info("Shipment delivered | orderId={}", order.getId());
            });

            case CANCELLED -> opt.ifPresent(s -> {
                s.setStatus(STATUS_RETURNED);
                shipmentRepository.save(s);
                log.info("Shipment returned | orderId={}", order.getId());
            });

            default -> { /* PENDING — no shipment yet */ }
        }
    }

    /**
     * Admin sets tracking info after handing parcel to carrier.
     */
    @Transactional
    public Optional<Shipment> updateTracking(Long orderId, String trackingNumber, String carrier) {
        return shipmentRepository.findByOrderId(orderId)
                .map(s -> {
                    s.setTrackingNumber(trackingNumber);
                    s.setCarrier(carrier);
                    Shipment saved = shipmentRepository.save(s);
                    log.info("Shipment tracking updated | orderId={} | carrier={} | tracking={}",
                            orderId, carrier, trackingNumber);
                    return saved;
                });
    }

    public Optional<Shipment> findByOrder(Order order) {
        return shipmentRepository.findByOrder(order);
    }
}
