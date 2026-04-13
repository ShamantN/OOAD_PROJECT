package com.ecommerce.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;;

@Entity
@Table(name = "refunds") 
public class Refund {

    public Refund(){}

    // @Id marks refundId as the primary key for this table
    // @GeneratedValue(strategy=GenerationType.INDETITY) tells the framework to let mysql db 
    // generate ID values automactically(auto_increment)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int refundId;

    @Column(nullable=false)
    private double amount;

    @OneToOne
    @JoinColumn(name = "order_id",nullable=false,unique=true)
    @JsonIgnore
    private Order order;

    public void initiateRefund(){
        this.order.setStatus(OrderStatus.CANCELLED);
        System.out.println("Refund initiated for amount: " + this.amount);
    }

    public int getRefundId(){
        return refundId;
    }

    public void setAmount(double amount){this.amount = amount;}
    public double getAmount(){return amount;}

    public void setOrder(Order order){this.order = order;}
    public Order getOrder(){return order;}



}
