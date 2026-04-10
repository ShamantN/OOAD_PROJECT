package com.ecommerce.system.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

// CRITICAL: You must tell Spring this is a database table!
@Entity
@Table(name = "orders")
public class Order {
    
    // Required blank constructor
    public Order(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private OrderStatus status;

    // Renamed to totalAmount to match your OrderService
    @Column(nullable=false)
    private double totalAmount;

    @ManyToOne
    @JoinColumn(name="user_id",nullable=false)
    private User user;

    // Initialized the list to prevent NullPointerExceptions
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    // --- Business Methods from your Diagram ---
    public void placeOrder(){
        this.status = OrderStatus.CREATED;
    }
    
    public void cancelOrder(){
        this.status = OrderStatus.CANCELLED;
    }

    // --- Standard Getters and Setters ---
    
    public int getOrderId() { return orderId; }

    public void setStatus(OrderStatus status){ this.status = status; }
    public OrderStatus getStatus(){ return status; }

    public void setTotalAmount(double totalAmount){ this.totalAmount = totalAmount; }
    public double getTotalAmount(){ return totalAmount; }

    public void setUser(User user){ this.user = user; }
    public User getUser(){ return user; }

    public void setItems(List<OrderItem> items){ this.orderItems = items; }
    public List<OrderItem> getItems(){ return orderItems; }
}