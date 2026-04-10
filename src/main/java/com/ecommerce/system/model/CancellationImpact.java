package com.ecommerce.system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "cancellation_impacts")
public class CancellationImpact {

    // Added a Primary Key, as every database table requires one
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int impactId;

    @Column(nullable = false)
    private double refundableAmount;

    @Column(nullable = false)
    private double deliveryChargeLoss;

    @Column(nullable = false)
    private double cancellationFee;

    @Column(nullable = false)
    private double finalRefund;

    // Mapping the 1-to-1 relationship to Order
    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @JsonIgnore
    private Order order;

    // REQUIRED BLANK CONSTRUCTOR
    public CancellationImpact() {
    }

    // Method exactly from your 3rd diagram
    public void calculateImpact(Order order) {
        // Logic will calculate fees based on order's current status (e.g., if already SHIPPED)
        this.refundableAmount = order.getTotalAmount();
        this.finalRefund = this.refundableAmount - this.cancellationFee - this.deliveryChargeLoss;
    }

    // Standard Getters and Setters...
    public int getImpactId() { return impactId; }
    
    public void setRefundableAmount(double refundableAmount) { this.refundableAmount = refundableAmount; }
    public double getRefundableAmount() { return refundableAmount; }
    
    public void setDeliveryChargeLoss(double deliveryChargeLoss) { this.deliveryChargeLoss = deliveryChargeLoss; }
    public double getDeliveryChargeLoss() { return deliveryChargeLoss; }
    
    public void setCancellationFee(double cancellationFee) { this.cancellationFee = cancellationFee; }
    public double getCancellationFee() { return cancellationFee; }
    
    public void setFinalRefund(double finalRefund) { this.finalRefund = finalRefund; }
    public double getFinalRefund() { return finalRefund; }
    
    public void setOrder(Order order) { this.order = order; }
    public Order getOrder() { return order; }
}