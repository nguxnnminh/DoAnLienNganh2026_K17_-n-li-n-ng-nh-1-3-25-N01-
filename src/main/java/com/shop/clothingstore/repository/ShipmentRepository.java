package com.shop.clothingstore.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.Shipment;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrder(Order order);

    Optional<Shipment> findByOrderId(Long orderId);
}
