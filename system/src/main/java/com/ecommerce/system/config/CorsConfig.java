package com.ecommerce.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Apply CORS to all endpoints
        registry.addMapping("/**")
                // Allow our specific local frontend servers
                .allowedOrigins("http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:3000")
                // Allow the standard REST techniques
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Allow standard headers + our custom simulated RBAC header
                .allowedHeaders("Content-Type", "Admin-User-Id", "Authorization")
                .allowCredentials(true);
    }
}
