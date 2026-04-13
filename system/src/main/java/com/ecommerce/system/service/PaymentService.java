package com.ecommerce.system.service;

import com.ecommerce.system.model.Order;
import com.ecommerce.system.model.Payment;
import com.ecommerce.system.model.PaymentStatus;
import com.ecommerce.system.model.OrderStatus;
import com.ecommerce.system.repository.OrderRepository;
import com.ecommerce.system.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    // --- OOAD Concept: State Transitions ---
    // In Object-Oriented Analysis and Design, objects move through a lifecycle defined by "States".
    // A "State Transition" occurs when an event (like receiving a payment) triggers rules that 
    // legally change the object from State A to State B.
    // 
    // Here, we model two distinct state-machines:
    // 1. Payment: FAILED -> SUCCESS
    // 2. Order: CREATED -> PAID
    // 
    // We encapsulate the validation logic (amount == totalAmount) to ensure invalid state 
    // transitions (e.g., partially paid orders becoming PAID) are impossible.
    @Transactional
    public void processPayment(int orderId, double paymentAmount) {
        
        // 1. Look up the Order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // 2. Fetch the corresponding pending/failed Payment record using the mapping
        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("No payment record associated with this order."));

        // 3. Validation rule to confirm state transition eligibility
        if (order.getTotalAmount() != paymentAmount) {
            // State remains FAILED (or default), exception thrown
            throw new RuntimeException("Payment amount (" + paymentAmount + ") does not match order total (" + order.getTotalAmount() + "). Transaction failed.");
        }

        // 4. Update states (State Transition Execution)
        payment.processPayment(); // Internally updates to PaymentStatus.SUCCESS
        order.setStatus(OrderStatus.PAID);
        
        // 5. Persist changes to Database
        paymentRepository.save(payment);
        orderRepository.save(order);
    }
}
