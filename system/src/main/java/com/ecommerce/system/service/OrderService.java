package com.ecommerce.system.service;

import com.ecommerce.system.dto.CancellationImpactDTO;
import com.ecommerce.system.model.*;
import com.ecommerce.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    // 1. Dependency Injection: Bringing in the database bridges
    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CancellationImpactRepository impactRepository;
    @Autowired private ProductRepository productRepository; // Fetch true product details

    // --- USE CASE: PLACE ORDER ---
    @Transactional
    public Order processNewOrder(Order incomingOrder) {
        double calculatedTotal = 0.0;

        // Loop through the items the customer wants to buy
        for (OrderItem item : incomingOrder.getItems()) {
            
            // Rehydrate the true Product from the database so we have its Name and properties 
            // instead of just trusting the ID passed from the frontend JSON payload.
            Product realProduct = productRepository.findById(item.getProduct().getProductId())
                    .orElseThrow(() -> new RuntimeException("Invalid Product ID"));
            item.setProduct(realProduct);
            
            // SECURITY FIX: Never trust the frontend's price! Always use the DB price.
            item.setPrice(realProduct.getPrice());
            
            // Check Product Availability
            Inventory inventory = inventoryRepository.findByProduct(realProduct);
            
            if (inventory == null || !inventory.isAvailable(item.getQuantity())) {
                // If out of stock, crash this process before anything saves
                throw new RuntimeException("Sorry, " + realProduct.getName() + " is out of stock!");
            }

            // Reduce stock and save it to the database
            inventory.reduceStock(item.getQuantity());
            inventoryRepository.save(inventory); 

            // Calculate money and link the item to this specific order
            calculatedTotal += item.calculateItemTotal();
            item.setOrder(incomingOrder); 
        }

        // Finalize the Order details
        incomingOrder.setTotalAmount(calculatedTotal);
        incomingOrder.placeOrder(); // This uses your Model method to set status to CREATED
        
        // Save the order FIRST so MySQL generates the Primary Key (orderId)
        Order savedOrder = orderRepository.save(incomingOrder);

        // Generate the pending Payment Record (1-to-1 relationship)
        Payment newPayment = new Payment();
        newPayment.setAmount(calculatedTotal);
        newPayment.setStatus(PaymentStatus.FAILED); // Default to failed until actually paid
        newPayment.setOrder(savedOrder);
        paymentRepository.save(newPayment);

        return savedOrder;
    }

    // --- USE CASE: CANCEL ORDER ---
    @Transactional
    public CancellationImpact cancelOrder(int orderId) {
        
        // Find the order by its ID
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found!"));

        // Business Rule: Cannot cancel if already delivered
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Delivered orders cannot be canceled, only returned.");
        }

        // Business Rule: Cannot cancel if already cancelled
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("This order is already cancelled.");
        }

        // Fetch the Payment associated with the order (Step 2.1)
        Payment payment = paymentRepository.findByOrder(order)
            .orElseThrow(() -> new RuntimeException("No payment found for this order"));

        CancellationImpact impact = new CancellationImpact();

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            // Condition B (Paid Order): Execute normal refund math (Step 2.3)
            if (order.getStatus() == OrderStatus.SHIPPED) {
                // In-delivery cancellation: 15% penalty + flat $10 delivery charge loss
                impact.setCancellationFee(order.getTotalAmount() * 0.15);
                impact.setDeliveryChargeLoss(10.0);
            } else {
                // PAID, but not yet shipped: Full refund, no penalties
                impact.setCancellationFee(0.0);
                impact.setDeliveryChargeLoss(0.0);
            }
            impact.calculateImpact(order);
        } else {
            // Condition A (Unpaid Order): Set everything to 0 (Step 2.2)
            impact.setCancellationFee(0.0);
            impact.setDeliveryChargeLoss(0.0);
            impact.setRefundableAmount(0.0);
            impact.setFinalRefund(0.0);
        }
        
        impact.setOrder(order);
        
        // Update Order Status to CANCELLED
        order.cancelOrder(); 
        orderRepository.save(order);
        
        // RESTORE THE INVENTORY (Crucial for real-world logic)
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByProduct(item.getProduct());
            if (inventory != null) {
                inventory.increaseStock(item.getQuantity()); // Put items back on the shelf
                inventoryRepository.save(inventory);
            }
        }

        // Save and return the cancellation receipt
        return impactRepository.save(impact);
    }

    // =========================================================================
    // --- USE CASE: VIEW CANCELLATION RECEIPT (for a specific order) ---
    // =========================================================================
    /**
     * Fetches the cancellation receipt for a single order and maps it to a safe DTO.
     *
     * @Transactional(readOnly = true) skips the dirty-checking overhead of a
     * write transaction — a clean performance optimisation for read-only paths.
     *
     * @param orderId  The order whose receipt the user wants to view.
     * @return         A flat CancellationImpactDTO with no nested entity objects.
     * @throws RuntimeException if no cancellation record exists for this order yet.
     */
    @Transactional(readOnly = true)
    public CancellationImpactDTO getImpactByOrderId(int orderId) {
        CancellationImpact impact = impactRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "No cancellation record found for Order ID: " + orderId +
                        ". Has this order been cancelled yet?"));
        return toDTO(impact);
    }

    // =========================================================================
    // --- USE CASE: ADMIN — VIEW ALL CANCELLATION IMPACTS ---
    // =========================================================================
    /**
     * Fetches ALL cancellation impact records and returns them as a clean List of DTOs.
     * This powers the Admin's system-wide penalty report.
     *
     * @return  A List<CancellationImpactDTO> — flat, safe, and recursion-free.
     */
    @Transactional(readOnly = true)
    public List<CancellationImpactDTO> getAllCancellationImpacts() {
        return impactRepository.findAll()          // Fetch every row from cancellation_impacts
                .stream()                          // Stream for functional-style mapping
                .map(this::toDTO)                  // Convert each entity -> DTO
                .collect(Collectors.toList());     // Collect back to a List
    }

    // =========================================================================
    // --- USE CASE: FETCH USER ORDERS (Returns DTO with Payment Status) ---
    // =========================================================================
    @Transactional(readOnly = true)
    public List<com.ecommerce.system.dto.OrderResponseDTO> getUserOrders(int userId) {
        return orderRepository.findByUserUserId(userId)
                .stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());
    }

    private com.ecommerce.system.dto.OrderResponseDTO toOrderDTO(Order order) {
        // Fetch the corresponding payment to get its status
        Payment payment = paymentRepository.findByOrder(order).orElse(null);
        String paymentStatus = (payment != null) ? payment.getStatus().name() : "UNKNOWN";

        return new com.ecommerce.system.dto.OrderResponseDTO(
                order.getOrderId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                paymentStatus,
                order.getItems()
        );
    }
    
    // =========================================================================
    // --- PRIVATE MAPPING HELPER ---
    // =========================================================================
    /**
     * Converts a CancellationImpact entity into a CancellationImpactDTO.
     */
    private CancellationImpactDTO toDTO(CancellationImpact impact) {
        return new CancellationImpactDTO(
                impact.getImpactId(),
                impact.getOrder().getOrderId(),   // Extract just the FK integer, not the whole Order
                impact.getCancellationFee(),
                impact.getDeliveryChargeLoss(),
                impact.getRefundableAmount(),
                impact.getFinalRefund()
        );
    }
}