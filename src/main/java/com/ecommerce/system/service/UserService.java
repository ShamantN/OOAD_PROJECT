package com.ecommerce.system.service;

import com.ecommerce.system.dto.LoginDTO;
import com.ecommerce.system.dto.UserRegistrationDTO;
import com.ecommerce.system.dto.UserResponseDTO;
import com.ecommerce.system.model.User;
import com.ecommerce.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponseDTO registerUser(UserRegistrationDTO dto) {
        // Check if user with that email already exists
        Optional<User> existingUser = userRepository.findByEmail(dto.getEmail());
        if (existingUser.isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        // Map DTO to real User entity
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        
        // Note: For this lab, we are storing the password as plain text.
        // In a production app, you MUST hash the password using a library like BCrypt.
        // E.g., user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPassword(dto.getPassword());
        user.setRole(dto.getRole());

        // Save to database
        User savedUser = userRepository.save(user);

        // Map saved User to UserResponseDTO
        return new UserResponseDTO(
                savedUser.getUserId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @Transactional(readOnly = true)
    public UserResponseDTO loginUser(LoginDTO dto) {
        // Find user by email
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Check if passwords match (plain text check for lab purposes)
        // In production, use: if (!passwordEncoder.matches(dto.getPassword(), user.getPassword()))
        if (!user.getPassword().equals(dto.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Return mapped UserResponseDTO
        return new UserResponseDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
