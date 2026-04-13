package com.ecommerce.system.repository;

import com.ecommerce.system.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Derived query: Spring generates SQL "SELECT * FROM orders WHERE user_id = ?"
    List<Order> findByUserUserId(int userId);
}