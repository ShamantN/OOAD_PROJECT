package com.ecommerce.system.model;
// above line tells the compiler exactly where the file lives in the projects dir structure


import jakarta.persistence.*;

public class User {

    public User(){}

    // @Id marks userId as the primary key for this table
    // @GeneratedValue(strategy=GenerationType.INDETITY) tells the framework to let mysql db 
    // generate ID values automactically(auto_increment)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int  userId;

    // @Column(nullable=false) is a constraint that translates to NOT NULL in the db
    @Column(nullable=false)
    private String name;

    //unique=true is a constraint that translates to UNIQUE in the db
    @Column(nullable=false,unique=true)
    private String email;

    @Column(nullable=false)
    private String password;

    //EnumType.STRING stores the specific enum val as a string in the db instead of a number
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Role role;

    public int getUserId(){
        return userId;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public void setEmail(String email){
        this.email = email;
    }
    
    public String getEmail(){
        return email;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public String getPassword(){
        return password;
    }

    public void setRole(Role role){
        this.role = role;
    }

    public Role getRole(){
        return role;
    }


}
