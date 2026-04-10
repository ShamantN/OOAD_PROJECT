package com.ecommerce.system.repository;

import com.ecommerce.system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    
    // Custom query: Spring writes the SQL to find a user by their unique email.
    // We use Optional<> because the database might not find a user with that email.
    Optional<User> findByEmail(String email);
}