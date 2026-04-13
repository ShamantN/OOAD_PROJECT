package com.ecommerce.system.controller;

import com.ecommerce.system.model.CancellationImpact;
import com.ecommerce.system.model.Order;
import com.ecommerce.system.repository.OrderRepository;
import com.ecommerce.system.service.OrderService;
import com.ecommerce.system.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    // --- ENDPOINT 1: Place a New Order ---
    // Web Address: POST http://localhost:8080/api/orders/place
    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(@RequestBody Order incomingOrder) {
        try {
            // Hand the web data to the Service to process
            Order completedOrder = orderService.processNewOrder(incomingOrder);
            
            // Send a 200 OK success response back to the website with the saved order
            return ResponseEntity.ok(completedOrder);
        } catch (RuntimeException e) {
            // If the Service throws an "Out of Stock" error, send a 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- ENDPOINT 2: Cancel an Order ---
    // Web Address: POST http://localhost:8080/api/orders/5/cancel (where 5 is the orderId)
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable int orderId) {
        try {
            // Hand the ID to the Service to calculate the impact and restore inventory
            CancellationImpact receipt = orderService.cancelOrder(orderId);
            
            // Send the receipt back to the user
            return ResponseEntity.ok(receipt);
        } catch (RuntimeException e) {
            // Catch errors like "Order already delivered"
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- ENDPOINT 4: Get Order History for a Specific User ---
    // Web Address: GET http://localhost:8080/api/orders/user/1
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getOrdersByUser(@PathVariable int userId) {
        List<Order> orders = orderRepository.findByUserUserId(userId);
        return ResponseEntity.ok(orders);
    }

    // --- ENDPOINT 3: Pay for an Order ---
    // Web Address: POST http://localhost:8080/api/orders/5/pay?amount=99.99
    // Concept: State Transition Endpoint (Triggers FAILED -> SUCCESS payment state and CREATED -> PAID order state)
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<?> payForOrder(@PathVariable int orderId, @RequestParam double amount) {
        try {
            // Hand the transaction to the PaymentService
            paymentService.processPayment(orderId, amount);
            
            // Return success message
            return ResponseEntity.ok("Payment successful. Order status updated to PAID.");
        } catch (RuntimeException e) {
            // Return the bad request if the payment failed (e.g., amount mismatch)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}