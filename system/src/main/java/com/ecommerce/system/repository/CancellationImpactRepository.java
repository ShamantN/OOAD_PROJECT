package com.ecommerce.system.repository;

import com.ecommerce.system.model.CancellationImpact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CancellationImpactRepository extends JpaRepository<CancellationImpact, Integer> {

    // JPQL aggregate query: sums all delivery losses across every cancellation record
    @Query("SELECT COALESCE(SUM(c.deliveryChargeLoss), 0) FROM CancellationImpact c")
    double sumTotalDeliveryChargeLoss();

    // JPQL aggregate query: sums all penalty fees collected across every cancellation record
    @Query("SELECT COALESCE(SUM(c.cancellationFee), 0) FROM CancellationImpact c")
    double sumTotalCancellationFees();

    /**
     * Spring Data JPA Derived Query.
     * Reads as: "Find a CancellationImpact WHERE its related Order's orderId equals the given value."
     * Spring auto-generates the JOIN SQL — no @Query annotation needed.
     * Returns Optional so the caller can throw a meaningful error if no record exists.
     */
    Optional<CancellationImpact> findByOrderOrderId(int orderId);
}