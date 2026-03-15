package com.ecommerce.system.model;

import jakarta.persistence.*;


// @Entity tells the framework that this class represents a table in the db
// @Table species the exact name of the table in the db


@Entity
@Table(name = "warehouses")
public class Warehouse {

    public Warehouse(){}
    
    // @Id marks warehouseId as the primary key for this table
    // @GeneratedValue(strategy=GenerationType.INDETITY) tells the framework to let mysql db 
    // generate ID values automactically(auto_increment))
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int warehouseId;

    // @Column(nullable=false) is a constraint that translates to NOT NULL in the database

    @Column(nullable=false)
    private String location;

    @Column(nullable=false)
    private String warehouseName;

    //----------------------------------------------------------------------------------------------

    public int getWarehouseId(){
        return warehouseId;
    }

    public void setWarehouseName(String name){
        this.warehouseName = name;
    }

    public String getWarehouseName(){
        return warehouseName;
    }  

    public void setLocation(String location){
        this.location = location;
    }

    public String getLocation(){
        return location;
    }   
        
}
