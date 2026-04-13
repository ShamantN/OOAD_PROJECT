package com.ecommerce.system.controller;

import com.ecommerce.system.dto.AdminMetricsDTO;
import com.ecommerce.system.model.Order;
import com.ecommerce.system.model.Product;
import com.ecommerce.system.model.Role;
import com.ecommerce.system.model.User;
import com.ecommerce.system.repository.CancellationImpactRepository;
import com.ecommerce.system.repository.OrderRepository;
import com.ecommerce.system.repository.UserRepository;
import com.ecommerce.system.service.FulfillmentService;
import com.ecommerce.system.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ProductService productService;
    private final UserRepository userRepository;
    private final FulfillmentService fulfillmentService;
    private final OrderRepository orderRepository;
    private final CancellationImpactRepository cancellationImpactRepository;

    @Autowired
    public AdminController(ProductService productService, UserRepository userRepository,
                           FulfillmentService fulfillmentService, OrderRepository orderRepository,
                           CancellationImpactRepository cancellationImpactRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
        this.fulfillmentService = fulfillmentService;
        this.orderRepository = orderRepository;
        this.cancellationImpactRepository = cancellationImpactRepository;
    }

    // --- OOAD Concept: Authorization / RBAC ---
    // Instead of using a complex framework, we use a required Header parameter to simulate an auth token.
    // We encapsulate the validation rule: "Only users that actually exist AND hold the ADMIN or 
    // INVENTORY_MANAGER role can perform these state-modifying actions."
    // If the check fails, we immediately throw a 403 Forbidden exception to halt execution.
    private void verifyAdminAccess(int adminId) {
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: User not found."));

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.INVENTORY_MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Insufficient permissions.");
        }
    }

    @PostMapping("/products")
    public ResponseEntity<Product> addProduct(
            @RequestHeader(value = "Admin-User-Id", required = true) int adminId,
            @Valid @RequestBody Product product) {
        
        verifyAdminAccess(adminId); // Guard the endpoint
        
        Product savedProduct = productService.addNewProduct(product);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }

    // --- Admin Cancellation Metrics Endpoint ---
    @GetMapping("/metrics/cancellations")
    public ResponseEntity<AdminMetricsDTO> getCancellationMetrics(
            @RequestHeader(value = "Admin-User-Id", required = true) int adminId) {
        verifyAdminAccess(adminId);

        double deliveryLoss = cancellationImpactRepository.sumTotalDeliveryChargeLoss();
        double feesCollected = cancellationImpactRepository.sumTotalCancellationFees();
        long cancelledCount = cancellationImpactRepository.count();

        return ResponseEntity.ok(new AdminMetricsDTO(deliveryLoss, feesCollected, cancelledCount));
    }

    // --- Admin Order View Endpoint ---
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders(
            @RequestHeader(value = "Admin-User-Id", required = true) int adminId) {
        verifyAdminAccess(adminId);
        return ResponseEntity.ok(orderRepository.findAll());
    }

    @PutMapping("/inventory/restock")
    public ResponseEntity<Map<String, String>> restockInventory(
            @RequestHeader(value = "Admin-User-Id", required = true) int adminId,
            @RequestBody RestockRequest request) {
        
        verifyAdminAccess(adminId); // Guard the endpoint
        
        productService.restockInventory(request.getProductId(), request.getWarehouseId(), request.getQuantityToAdd());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Stock successfully updated.");
        return ResponseEntity.ok(response);
    }

    // --- Fulfillment Endpoints ---

    @PutMapping("/orders/{orderId}/ship")
    public ResponseEntity<Map<String, String>> shipOrder(
            @RequestHeader(value = "Admin-User-Id", required = true) int adminId,
            @PathVariable int orderId) {
        
        verifyAdminAccess(adminId); // Guard the endpoint
        
        fulfillmentService.shipOrder(orderId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Order " + orderId + " has been successfully shipped.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/orders/{orderId}/deliver")
    public ResponseEntity<Map<String, String>> deliverOrder(
            @RequestHeader(value = "Admin-User-Id", required = true) int adminId,
            @PathVariable int orderId) {
        
        verifyAdminAccess(adminId); // Guard the endpoint
        
        fulfillmentService.deliverOrder(orderId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Order " + orderId + " has been successfully delivered.");
        return ResponseEntity.ok(response);
    }

    public static class RestockRequest {
        private Integer productId;
        private Integer warehouseId;
        private Integer quantityToAdd;

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }
        public Integer getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Integer warehouseId) { this.warehouseId = warehouseId; }
        public Integer getQuantityToAdd() { return quantityToAdd; }
        public void setQuantityToAdd(Integer quantityToAdd) { this.quantityToAdd = quantityToAdd; }
    }

    // Generic exception handling for 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    // Fallback for our manual RuntimeExceptions thrown by FulfillmentService
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}
