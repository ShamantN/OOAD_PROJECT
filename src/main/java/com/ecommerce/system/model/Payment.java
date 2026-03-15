package com.ecommerce.system.model;

import jakarta.persistence.*;

public class Payment {

    public Payment(){}

    // @Id marks paymentId as the primary key for this table
    // @GeneratedValue(strategy=GenerationType.INDETITY) tells the framework to let mysql db 
    // generate ID values automactically(auto_increment)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int paymentId;

    @Column(nullable=false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private PaymentStatus status;

    @OneToOne
    @JoinColumn(name="order_id",nullable=false,unique=true)
    private Order order;

    public void processPayment(){
        this.status = PaymentStatus.SUCCESS;
    }

    public void setAmount(double amount){this.amount = amount;}
    public double getAmount(){return amount;}

    public void setStatus(PaymentStatus status){this.status = status;}
    public PaymentStatus getStatus(){return status;}

    public void setOrder(Order order){this.order = order;}
    public Order getOrder(){return order;}



}
