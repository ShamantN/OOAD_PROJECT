package com.ecommerce.system.dto;

import com.ecommerce.system.model.OrderItem;
import java.util.List;

public class OrderResponseDTO {
    private int orderId;
    private String status;
    private double totalAmount;
    private String paymentStatus;
    private List<OrderItem> items;

    public OrderResponseDTO() {}

    public OrderResponseDTO(int orderId, String status, double totalAmount, String paymentStatus, List<OrderItem> items) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.items = items;
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
