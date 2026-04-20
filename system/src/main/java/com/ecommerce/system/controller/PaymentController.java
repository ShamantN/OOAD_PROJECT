package com.ecommerce.system.controller;

import com.ecommerce.system.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // --- Endpoint: Simulate Payment ---
    // Web Address: POST http://localhost:8080/api/payments/{orderId}/pay
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<Map<String, String>> processPayment(@PathVariable int orderId) {
        try {
            paymentService.processPayment(orderId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Payment successfully processed. Order status updated to PAID.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
