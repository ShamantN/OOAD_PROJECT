package com.ecommerce.system.service;

import com.ecommerce.system.model.Inventory;
import com.ecommerce.system.model.Product;
import com.ecommerce.system.model.Warehouse;
import com.ecommerce.system.repository.InventoryRepository;
import com.ecommerce.system.repository.ProductRepository;
import com.ecommerce.system.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;

    @Autowired
    public ProductService(ProductRepository productRepository,
                          InventoryRepository inventoryRepository,
                          WarehouseRepository warehouseRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public Product addNewProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void restockInventory(Integer productId, Integer warehouseId, Integer quantityToAdd) {
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Quantity to add must be greater than zero.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Product ID"));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Warehouse ID"));

        Optional<Inventory> optInventory = inventoryRepository.findByProductAndWarehouse(product, warehouse);

        if (optInventory.isPresent()) {
            Inventory inventory = optInventory.get();
            inventory.increaseStock(quantityToAdd);
            inventoryRepository.save(inventory);
        } else {
            Inventory newInventory = new Inventory();
            newInventory.setProduct(product);
            newInventory.setWarehouse(warehouse);
            newInventory.setStock(quantityToAdd);
            inventoryRepository.save(newInventory);
        }
    }
}
