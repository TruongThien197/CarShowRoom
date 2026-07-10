package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.ReservationStatus;
import com.hsf302.carshowroom.entity.InventoryReservation;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.OrderItem;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.repository.InventoryReservationRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryReservationServiceImpl implements InventoryReservationService {
    private static final int PAYMENT_HOLD_MINUTES = 15;

    private final ProductRepository productRepository;
    private final InventoryReservationRepository reservationRepository;

    @Override
    public void checkStockAvailability(Product product, int quantity) {
        if (product.getAvailableStock() < quantity) {
            throw new RuntimeException("Not enough available stock for " + product.getProductName() + ".");
        }
    }

    @Override
    @Transactional
    public void reserveStock(List<Order> orders) {
        for (Order order : orders) {
            reserveStock(order);
        }
    }

    private void reserveStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found."));
            checkStockAvailability(product, item.getQuantity());
            product.setReservedStock(product.getReservedStock() + item.getQuantity());
            productRepository.save(product);

            InventoryReservation reservation = new InventoryReservation();
            reservation.setOrder(order);
            reservation.setProduct(product);
            reservation.setQuantity(item.getQuantity());
            reservation.setReservationStatus(ReservationStatus.HELD);
            reservation.setExpiresAt(LocalDateTime.now().plusMinutes(PAYMENT_HOLD_MINUTES));
            reservationRepository.save(reservation);
        }
    }

    @Override
    @Transactional
    public void confirmReservation(Order order) {
        for (InventoryReservation reservation : activeReservations(order)) {
            reservation.setReservationStatus(ReservationStatus.CONFIRMED);
            reservation.setExpiresAt(null);
            reservationRepository.save(reservation);
        }
    }

    @Override
    @Transactional
    public void releaseReservation(Order order) {
        for (InventoryReservation reservation : activeReservations(order)) {
            Product product = productRepository.findByIdForUpdate(reservation.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found."));
            product.setReservedStock(Math.max(0, product.getReservedStock() - reservation.getQuantity()));
            productRepository.save(product);
            reservation.setReservationStatus(ReservationStatus.RELEASED);
            reservationRepository.save(reservation);
        }
    }

    @Override
    @Transactional
    public void consumeStock(Order order) {
        for (InventoryReservation reservation : activeReservations(order)) {
            Product product = productRepository.findByIdForUpdate(reservation.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found."));
            product.setPhysicalStock(Math.max(0, product.getPhysicalStock() - reservation.getQuantity()));
            product.setReservedStock(Math.max(0, product.getReservedStock() - reservation.getQuantity()));
            productRepository.save(product);
            reservation.setReservationStatus(ReservationStatus.CONSUMED);
            reservationRepository.save(reservation);
        }
    }

    private List<InventoryReservation> activeReservations(Order order) {
        return reservationRepository.findByOrderAndReservationStatusIn(
                order,
                List.of(ReservationStatus.HELD, ReservationStatus.CONFIRMED)
        );
    }
}
