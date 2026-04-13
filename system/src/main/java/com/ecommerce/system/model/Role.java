package com.ecommerce.system.model;
// above line tells the compiler exactly where the file lives in the projects dir structure

//enum is used to define a class that represents a group of constants
//java treats enum as a class that extends java.lang.Enum
//creates one isntance of each value inside the {}
// the constants are automatically made public static final
public enum Role {
    CUSTOMER,
    INVENTORY_MANAGER,
    WAREHOUSE_MANAGER,
    ADMIN
}
