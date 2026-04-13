package com.ecommerce.system.dto;

/**
 * Data Transfer Object for Admin Financial Metrics.
 * Returned by GET /api/admin/metrics/cancellations.
 * Aggregates total delivery charge losses and total cancellation fees
 * across all cancelled orders in the system.
 */
public class AdminMetricsDTO {

    private double totalDeliveryChargeLoss;
    private double totalCancellationFeesCollected;
    private long   totalCancelledOrders;

    public AdminMetricsDTO() {}

    public AdminMetricsDTO(double totalDeliveryChargeLoss, double totalCancellationFeesCollected, long totalCancelledOrders) {
        this.totalDeliveryChargeLoss          = totalDeliveryChargeLoss;
        this.totalCancellationFeesCollected   = totalCancellationFeesCollected;
        this.totalCancelledOrders             = totalCancelledOrders;
    }

    public double getTotalDeliveryChargeLoss()           { return totalDeliveryChargeLoss; }
    public void   setTotalDeliveryChargeLoss(double v)   { this.totalDeliveryChargeLoss = v; }

    public double getTotalCancellationFeesCollected()         { return totalCancellationFeesCollected; }
    public void   setTotalCancellationFeesCollected(double v) { this.totalCancellationFeesCollected = v; }

    public long getTotalCancelledOrders()          { return totalCancelledOrders; }
    public void setTotalCancelledOrders(long v)    { this.totalCancelledOrders = v; }
}
