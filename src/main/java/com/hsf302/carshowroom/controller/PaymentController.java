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
            attributes.addFlashAttribute("errorMessage",
                    "PayOS did not return an order code. Please check the order payment status again.");
            return "redirect:/orders";
        }
        try {
            PaymentTransaction transaction = paymentService.syncPaymentStatus(orderCode);
            if (transaction.getStatus().name().equals("CANCELED")) {
                attributes.addFlashAttribute("errorMessage", "Bạn đã hủy thanh toán PayOS. Vui lòng chọn lại phương thức thanh toán.");
                return "redirect:/orders/" + paymentService.getOrderIdByPayOSCode(orderCode) + "/payment";
            }
            attributes.addFlashAttribute("successMessage",
                    transaction.getStatus().name().equals("PAID")
                            ? "PayOS payment successful."
                            : "Transaction is being processed by PayOS.");
        } catch (Exception exception) {
            attributes.addFlashAttribute("errorMessage",
                    "Unable to confirm payment: " + exception.getMessage());
        }
        return "redirect:/orders";
    }

    @GetMapping("/cancel")
    public String paymentCancel(@RequestParam(required = false) String orderCode,
                                RedirectAttributes attributes) {
        Integer orderId = null;
        if (orderCode != null && !orderCode.isBlank()) {
            try {
                orderId = paymentService.getOrderIdByPayOSCode(orderCode);
                paymentService.syncPaymentStatus(orderCode);
            } catch (Exception ignored) {
                // Keep a safe redirect even if PayOS is temporarily unavailable.
            }
        }
        attributes.addFlashAttribute("errorMessage", "Bạn đã hủy thanh toán PayOS. Vui lòng chọn lại phương thức thanh toán.");
        if (orderId != null) {
            return "redirect:/orders/" + orderId + "/payment";
        }
        return "redirect:/orders";
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
