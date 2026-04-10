package com.ecommerce.system.repository;

import com.ecommerce.system.model.Inventory;
import com.ecommerce.system.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {
    
    // Spring automatically translates this method name into a SQL query!
    Inventory findByProduct(Product product);
}