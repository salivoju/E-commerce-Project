package com.ecommerce.user_service;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
// We specify the table name "users" because "user" is often a reserved keyword in SQL.
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    // In a real application, this would be securely hashed!
    // For now, we will store it as plain text for simplicity.
    private String password;
}
