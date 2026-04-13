package com.ecommerce.system.service;

import com.ecommerce.system.dto.ProductCatalogDTO;
import com.ecommerce.system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final ProductRepository productRepository;

    @Autowired
    public CatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductCatalogDTO> getAvailableProducts() {
        return productRepository.findAvailableProducts();
    }
}
