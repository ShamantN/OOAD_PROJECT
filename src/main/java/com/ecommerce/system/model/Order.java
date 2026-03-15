package com.ecommerce.system.model;



import jakarta.persistence.*;
import java.util.List;

public class Order {
    
    public Order(){}


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private OrderStatus status;

    @Column(nullable=false)
    private double amount;

    @ManyToOne
    @JoinColumn(name="user_id",nullable=false)
    private User user;

    //@OneToMany(mappedBy = "order",cascade = CascadeType.ALL) tells the framework that it is the parent table and 
    // to use the order variable in OrderItems.java to find the db link
    // cascade=CascadeType.ALL: if u save an order in the db, it will save all the OrderItems associated with it as well
    @OneToMany(mappedBy = "order",cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    public double getTotalAmount(){
        double total = 0.0;
        for (OrderItem item : orderItems){
            total += item.calculateItemTotal();
        }
        return total;
    }

    public void placeOrder(){
        this.status = OrderStatus.CREATED;
    }
    public void cancelOrder(){
        this.status = OrderStatus.CANCELLED;
    }

    public void updateStatus(OrderStatus newStatus){
        this.status = newStatus;
    }

    public void setAmount(double amount){this.amount = amount;}
    public double getAmount(){return amount;}

    public void setUser(User user){this.user = user;}
    public User getUser(){return user;}

    public void setItems(List<OrderItem> items){this.orderItems = items;}
    public List<OrderItem> getItems(){return orderItems;}

}
