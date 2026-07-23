package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.ReservationStatus;
import com.hsf302.carshowroom.entity.InventoryReservation;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.OrderItem;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.exception.InsufficientStockException;
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

    /** Kiểm tra số lượng yêu cầu có còn khả dụng sau khi trừ hàng đang được giữ chờ thanh toán. */
    @Override
    public void checkStockAvailability(Product product, int quantity) {
        if (isInsufficient(product, quantity)) {
            throw new InsufficientStockException(product.getAvailableStock());
        }
        if (product.getAvailableStock() < quantity) {
            throw new RuntimeException("Không đủ tồn kho cho " + product.getProductName() + ".");
        }
    }

    /** Xác định nhanh yêu cầu có vượt quá tồn kho khả dụng của sản phẩm hay không. */
    private boolean isInsufficient(Product product, int quantity) {
        return product.getAvailableStock() < quantity;
    }

    /** Tạo các bản ghi giữ hàng cho toàn bộ đơn con trong lúc chờ thanh toán. */
    @Override
    @Transactional
    public void reserveStock(List<Order> orders) {
        for (Order order : orders) {
            reserveStock(order);
        }
    }

    /** Khóa từng sản phẩm, tăng số lượng đã giữ và tạo phiếu giữ hàng cho một đơn. */
    private void reserveStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));
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

    /** Xác nhận các phiếu giữ hàng sau khi thanh toán thành công. */
    @Override
    @Transactional
    public void confirmReservation(Order order) {
        for (InventoryReservation reservation : activeReservations(order)) {
            reservation.setReservationStatus(ReservationStatus.CONFIRMED);
            reservation.setExpiresAt(null);
            reservationRepository.save(reservation);
        }
    }

    /** Hoàn lại lượng hàng đã giữ khi đơn bị hủy hoặc quá hạn thanh toán. */
    @Override
    @Transactional
    public void releaseReservation(Order order) {
        for (InventoryReservation reservation : activeReservations(order)) {
            Product product = productRepository.findByIdForUpdate(reservation.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));
            product.setReservedStock(Math.max(0, product.getReservedStock() - reservation.getQuantity()));
            productRepository.save(product);
            reservation.setReservationStatus(ReservationStatus.RELEASED);
            reservationRepository.save(reservation);
        }
    }

    /** Trừ tồn kho thực tế khi đơn hoàn tất và đánh dấu phiếu giữ hàng đã sử dụng. */
    @Override
    @Transactional
    public void consumeStock(Order order) {
        for (InventoryReservation reservation : activeReservations(order)) {
            Product product = productRepository.findByIdForUpdate(reservation.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));
            product.setPhysicalStock(Math.max(0, product.getPhysicalStock() - reservation.getQuantity()));
            product.setReservedStock(Math.max(0, product.getReservedStock() - reservation.getQuantity()));
            productRepository.save(product);
            reservation.setReservationStatus(ReservationStatus.CONSUMED);
            reservationRepository.save(reservation);
        }
    }

    /** Lấy các phiếu giữ hàng vẫn còn hiệu lực của một đơn. */
    private List<InventoryReservation> activeReservations(Order order) {
        return reservationRepository.findByOrderAndReservationStatusIn(
                order,
                List.of(ReservationStatus.HELD, ReservationStatus.CONFIRMED)
        );
    }
}
