package com.ecommerce.system.dto;

/**
 * CancellationImpactDTO — Data Transfer Object for the Cancellation Receipt.
 *
 * WHY THIS EXISTS:
 * The raw CancellationImpact *entity* has a @OneToOne relationship back to the Order,
 * which itself has a @OneToMany to OrderItems, which each have a @ManyToOne to Product, etc.
 * Serializing that directly to JSON would produce a massive, deeply-nested object (and
 * potentially trigger infinite recursion before Jackson can catch it).
 *
 * This DTO acts as a "view model": we cherry-pick ONLY the flat, numeric fields the
 * client actually needs. The result is a small, clean, predictable JSON payload with
 * zero risk of circular references or over-exposure of internal data.
 */
public class CancellationImpactDTO {

    /** Primary key of the CancellationImpact record */
    private int impactId;

    /** The Order this impact belongs to (foreign-key value only — no nested object) */
    private int orderId;

    /** Penalty fee charged (15% of order total if shipped, 0 otherwise) */
    private double cancellationFee;

    /** Flat delivery charge that cannot be recovered once the order is in transit */
    private double deliveryChargeLoss;

    /** The gross order total (before deductions) — what the customer originally paid */
    private double refundableAmount;

    /** Net money that will actually be returned: refundableAmount - cancellationFee - deliveryChargeLoss */
    private double finalRefund;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Required no-arg constructor (good practice for frameworks). */
    public CancellationImpactDTO() {}

    /**
     * All-args convenience constructor used in the mapping helper inside OrderService.
     * Having this here keeps the mapping code concise and readable.
     */
    public CancellationImpactDTO(int impactId, int orderId,
                                  double cancellationFee, double deliveryChargeLoss,
                                  double refundableAmount, double finalRefund) {
        this.impactId          = impactId;
        this.orderId           = orderId;
        this.cancellationFee   = cancellationFee;
        this.deliveryChargeLoss = deliveryChargeLoss;
        this.refundableAmount  = refundableAmount;
        this.finalRefund       = finalRefund;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getImpactId()                         { return impactId; }
    public void setImpactId(int impactId)            { this.impactId = impactId; }

    public int getOrderId()                          { return orderId; }
    public void setOrderId(int orderId)              { this.orderId = orderId; }

    public double getCancellationFee()               { return cancellationFee; }
    public void setCancellationFee(double f)         { this.cancellationFee = f; }

    public double getDeliveryChargeLoss()            { return deliveryChargeLoss; }
    public void setDeliveryChargeLoss(double d)      { this.deliveryChargeLoss = d; }

    public double getRefundableAmount()              { return refundableAmount; }
    public void setRefundableAmount(double r)        { this.refundableAmount = r; }

    public double getFinalRefund()                   { return finalRefund; }
    public void setFinalRefund(double f)             { this.finalRefund = f; }
}
