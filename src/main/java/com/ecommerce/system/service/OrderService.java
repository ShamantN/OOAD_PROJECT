package com.ecommerce.system.service;

import com.ecommerce.system.model.*;
import com.ecommerce.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    // 1. Dependency Injection: Bringing in the database bridges
    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CancellationImpactRepository impactRepository;

    // --- USE CASE: PLACE ORDER ---
    @Transactional
    public Order processNewOrder(Order incomingOrder) {
        double calculatedTotal = 0.0;

        // Loop through the items the customer wants to buy
        for (OrderItem item : incomingOrder.getItems()) {
            
            // Check Product Availability
            Inventory inventory = inventoryRepository.findByProduct(item.getProduct());
            
            if (inventory == null || !inventory.isAvailable(item.getQuantity())) {
                // If out of stock, crash this process before anything saves
                throw new RuntimeException("Sorry, " + item.getProduct().getName() + " is out of stock!");
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

        // Business Rule: Cannot cancel if already delivered or cancelled
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Order cannot be cancelled at this stage.");
        }

        // Calculate the financial impact
        CancellationImpact impact = new CancellationImpact();
        
        if (order.getStatus() == OrderStatus.SHIPPED) {
            impact.setCancellationFee(order.getTotalAmount() * 0.10); // 10% penalty
            impact.setDeliveryChargeLoss(50.0); // Flat shipping loss
        } else {
            impact.setCancellationFee(0.0);
            impact.setDeliveryChargeLoss(0.0);
        }
        
        impact.calculateImpact(order);
        impact.setOrder(order);
        
        // Update Order Status
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

        // Save and return the receipt
        return impactRepository.save(impact);
    }
}