package com.ecommerce.system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

// Entity tells the spring data jpa that this class represents a table in the db
// Table specifies the exact name of the table in the db
@Entity
@Table(name="order_items")
public class OrderItem {
    
    public OrderItem(){}

    // @Id marks orderItemId as the primary key for this table
    // @GeneratedValue(strategy=GenerationType.INDETITY) tells the framework to let mysql db 
    // generate ID values automactically(auto_increment)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderItemId;

    @Column(nullable=false)
    @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @Column(nullable=false)
    private double price;


    //@JoinColumn(name,nullable) tells mysql to create a col named order_id
    // to hold the FK linking to the order table
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="order_id",nullable=false)
    private Order order;

    //@JoinColumn(name,nullable) tells mysql to create a col named product_id
    // to hold the FK linking to the products table
    @ManyToOne
    @JoinColumn(name="product_id",nullable=false)
    private Product product;

    public double calculateItemTotal(){
        return this.price * this.quantity;
    }

    public void setQuantity(int quantity){this.quantity = quantity;}
    public int getQuantity(){return quantity;}

    public void setPrice(double price){this.price = price;}
    public double getPrice(){return price;}


    public void setOrder(Order order){this.order = order;}
    public Order getOrder(){return order;}

    public void setProduct(Product product){this.product = product;}
    public Product getProduct(){return product;}


}
