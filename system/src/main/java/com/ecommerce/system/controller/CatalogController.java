package com.ecommerce.system.controller;

import com.ecommerce.system.dto.ProductCatalogDTO;
import com.ecommerce.system.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    @Autowired
    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductCatalogDTO>> getAvailableProducts() {
        List<ProductCatalogDTO> catalog = catalogService.getAvailableProducts();
        return ResponseEntity.ok(catalog);
    }
}
