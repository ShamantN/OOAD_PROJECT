package com.ecommerce.system.model;

import jakarta.persistence.*;

// Entity tells the spring data jpa that this class represents a table in the db
// Table specifies the exact name of the table in the db
@Entity
@Table(name = "inventory")
public class Inventory {
    
    public Inventory(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int inventoryId;

    // @Column(nullable=false) is a constraint that translates to NOT NULL in the db
    @Column(nullable=false)
    private int stock;


    //@JoinColumn(name,nullable) tells mysql to create a col named product_id
    // to hold the FK linking to the products table from this classes table
    @ManyToOne
    @JoinColumn(name="product_id",nullable=false)
    private Product product;

    @ManyToOne
    @JoinColumn(name="warehouse_id",nullable=false)
    private Warehouse warehouse;

    public boolean isAvailable(int requiredQuantity){
        return this.stock >= requiredQuantity;
    }

    public void reduceStock(int quantity){
        if (this.isAvailable(quantity)){
            this.stock -= quantity;
        }
        else{
            throw new IllegalArgumentException("Insufficient stock available.");
        }
    }

    public void increaseStock(int quantity){
        this.stock += quantity;
    }

    public void setStock(int stock){this.stock = stock;}
    public int getStock(){return stock;}

    public void setProduct(Product product){this.product = product;}
    public Product getProduct(){return product;}

    public void setWarehouse(Warehouse warehouse){this.warehouse = warehouse;}
    public Warehouse getWarehouse(){return warehouse;}

}
