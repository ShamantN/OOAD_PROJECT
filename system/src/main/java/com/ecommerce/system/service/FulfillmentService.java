package com.ecommerce.system.service;

import com.ecommerce.system.model.Order;
import com.ecommerce.system.model.OrderStatus;
import com.ecommerce.system.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FulfillmentService {

    private final OrderRepository orderRepository;

    @Autowired
    public FulfillmentService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // --- OOAD Concept: Strict State Transitions ---
    // In our system, an Order has a specific lifecycle (CREATED -> PAID -> SHIPPED -> DELIVERED).
    // State transitions must be strictly enforced to prevent "impossible" actions.
    // For example, we cannot legally "ship" an order that hasn't been PAID. 
    // This encapsulation of logic inside the FulfillmentService protects the core 
    // business integrity of the application.
    @Transactional
    public void shipOrder(int orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Invalid state transition. Order can only be shipped if it is currently PAID. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);
    }

    // Following the same strict rules, an order can only be delivered if it is currently SHIPPED.
    @Transactional
    public void deliverOrder(int orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new RuntimeException("Invalid state transition. Order can only be delivered if it is currently SHIPPED. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }
}
