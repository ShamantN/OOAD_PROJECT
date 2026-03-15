package com.ecommerce.system.model;

import jakarta.persistence.*;

// Entity tells the spring data jpa that this class represents a table in the db
// Table specifies the exact name of the table in the db
@Entity
@Table(name = "products")
public class Product {
    
    public Product(){}
    // @Id marks productId as the primary key for this table
    // @GeneratedValue(strategy=GenerationType.INDETITY) tells the framework to let mysql db 
    // generate ID values automactically(auto_increment)
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int productId;


    // @Column(nullable=false) is a constraint that translates to NOT NULL in the db
    @Column(nullable=false)
    private String name;

    @Column(nullable=false)
    private double price;

    public void updatePrice(double newPrice){
        this.price = newPrice;
    }

    //getters and setters

    public int getProductId(){
        return productId;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public double getPrice(){
        return price;
    }
}
