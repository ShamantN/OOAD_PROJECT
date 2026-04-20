package com.ecommerce.system.controller;

import com.ecommerce.system.dto.CancellationImpactDTO;
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
    public ResponseEntity<?> placeOrder(@jakarta.validation.Valid @RequestBody Order incomingOrder) {
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
        List<com.ecommerce.system.dto.OrderResponseDTO> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    // Note: The pay endpoint has been refactored into PaymentController.

    // --- ENDPOINT 5: View Cancellation Receipt for a Specific Order ---
    // Web Address: GET http://localhost:8080/api/orders/{orderId}/impact
    //
    // Separation of Concerns: This is a GET (read), NOT the POST /cancel (write).
    // The "action" (canceling) and the "report" (viewing the impact) are deliberately
    // kept in separate endpoints. A user might cancel at 9am and view their receipt at 3pm.
    // Conflating them into a single endpoint would break that use-case.
    @GetMapping("/{orderId}/impact")
    public ResponseEntity<?> getImpactForOrder(@PathVariable int orderId) {
        try {
            // Delegate to the service; it fetches and maps the impact to a safe DTO
            CancellationImpactDTO impact = orderService.getImpactByOrderId(orderId);
            return ResponseEntity.ok(impact);
        } catch (RuntimeException e) {
            // Handles the case where the order was never cancelled (no impact record exists)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}