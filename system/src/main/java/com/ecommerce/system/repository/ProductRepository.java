package com.ecommerce.system.repository;

import com.ecommerce.system.model.Product;
import com.ecommerce.system.dto.ProductCatalogDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // --- OOAD Concept: Derived JPA Query via JPQL ---
    // JPQL (Java Persistence Query Language) operates on Java Entity classes rather than SQL tables.
    // In this query:
    // 1. "SELECT new ..." is a Constructor Expression. It takes the raw database results and 
    //    immediately instantiates our ProductCatalogDTO object, ensuring we don't expose private Entity details.
    // 2. "FROM Inventory i JOIN i.product p" leverages the Object-Oriented relationships defined in our 
    //    classes (the @ManyToOne mapping) rather than writing manual SQL ON clauses.
    // 3. We group by product properties and use "HAVING SUM(i.stock) > 0" to strictly filter out any 
    //    out-of-stock items at the database level for maximum backend efficiency.
    @Query("SELECT new com.ecommerce.system.dto.ProductCatalogDTO(p.productId, p.name, p.price, SUM(i.stock)) " +
           "FROM Inventory i JOIN i.product p " +
           "GROUP BY p.productId, p.name, p.price " +
           "HAVING SUM(i.stock) > 0")
    List<ProductCatalogDTO> findAvailableProducts();
}