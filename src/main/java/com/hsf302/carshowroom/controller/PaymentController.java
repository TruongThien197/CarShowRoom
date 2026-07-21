package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.dto.PayOS.PayOSWebhookRequest;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import com.hsf302.carshowroom.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/payments/payos")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/return")
    public String paymentReturn(@RequestParam(required = false) String orderCode, RedirectAttributes attributes) {
        if (orderCode == null || orderCode.isBlank()) {
            attributes.addFlashAttribute("errorMessage", "PayOS không trả về mã giao dịch.");
            return "redirect:/orders";
        }
        try {
            PaymentTransaction transaction = paymentService.syncPaymentStatus(orderCode);
            if (transaction.getStatus().name().equals("CANCELED")) {
                attributes.addFlashAttribute("errorMessage", "Bạn đã hủy thanh toán PayOS. Vui lòng thực hiện lại thanh toán.");
                if (isBookingOnly(transaction)) {
                    if ("REMAINING".equalsIgnoreCase(transaction.getPaymentPurpose())) {
                        return "redirect:/booking/" + transaction.getBooking().getId();
                    }
                    return "redirect:/booking/" + transaction.getBooking().getId() + "/payment";
                }
                return "redirect:/orders/" + paymentService.getOrderIdByPayOSCode(orderCode) + "/payment";
            }
            attributes.addFlashAttribute("successMessage",
                    transaction.getStatus().name().equals("PAID")
                            ? "Thanh toán PayOS thành công."
                            : "Giao dịch đang được PayOS xử lý.");
            if (isBookingOnly(transaction)) {
                return "redirect:/booking/" + transaction.getBooking().getId();
            }
        } catch (Exception exception) {
            attributes.addFlashAttribute("errorMessage", "Không thể xác nhận thanh toán: " + exception.getMessage());
        }
        return "redirect:/orders";
    }

    @GetMapping("/cancel")
    public String paymentCancel(@RequestParam(required = false) String orderCode,
                                RedirectAttributes attributes) {
        Integer orderId = null;
        Integer bookingId = null;
        if (orderCode != null && !orderCode.isBlank()) {
            try {
                PaymentTransaction transaction = paymentService.syncPaymentStatus(orderCode);
                bookingId = transaction.getBooking() == null ? null : transaction.getBooking().getId();
                if (!isBookingOnly(transaction)) {
                    orderId = paymentService.getOrderIdByPayOSCode(orderCode);
                }
            } catch (Exception ignored) {
                try {
                    bookingId = paymentService.getBookingIdByPayOSCode(orderCode);
                    if (bookingId == null) {
                        orderId = paymentService.getOrderIdByPayOSCode(orderCode);
                    }
                } catch (Exception ignoredAgain) {
                    // Keep a safe redirect if PayOS is temporarily unavailable.
                }
            }
        }
        attributes.addFlashAttribute("errorMessage", "Bạn đã hủy thanh toán PayOS. Vui lòng thực hiện lại thanh toán.");
        if (orderId != null) {
            return "redirect:/orders/" + orderId + "/payment";
        }
        if (bookingId != null) {
            try {
                PaymentTransaction transaction = paymentService.syncPaymentStatus(orderCode);
                if ("REMAINING".equalsIgnoreCase(transaction.getPaymentPurpose())) {
                    return "redirect:/booking/" + bookingId;
                }
            } catch (Exception ignored) {
                // Fall back to the deposit retry page if PayOS cannot be queried.
            }
            return "redirect:/booking/" + bookingId + "/payment";
        }
        return "redirect:/orders";
    }

    private boolean isBookingOnly(PaymentTransaction transaction) {
        return transaction.getBooking() != null
                && transaction.getOrder() == null
                && transaction.getParentOrder() == null;
    }

    @PostMapping("/sync")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sync(@RequestParam String orderCode) {
        PaymentTransaction transaction = paymentService.syncPaymentStatus(orderCode);
        return ResponseEntity.ok(Map.of(
                "orderCode", transaction.getPayosOrderCode(),
                "status", transaction.getStatus().name()));
    }

    @PostMapping("/webhook")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> webhook(@RequestBody PayOSWebhookRequest request) {
        paymentService.handlePayOSWebhook(request);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
