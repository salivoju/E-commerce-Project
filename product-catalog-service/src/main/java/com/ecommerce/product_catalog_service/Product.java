package com.ecommerce.product_catalog_service;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data; // Import Lombok's @Data annotation
import java.math.BigDecimal;

/**
 * This is our Entity class.
 * @Entity tells Spring Data JPA that this class maps to a database table.
 * @Table specifies the name of the table in the database.
 * @Data is a Lombok annotation that automatically generates getters, setters,
 * toString(), equals(), and hashCode() methods for us. It keeps our code clean.
 */
@Entity
@Table(name = "products")
@Data
public class Product {

    /**
     * @Id marks this field as the primary key.
     * @GeneratedValue tells JPA how the primary key is generated.
     * GenerationType.IDENTITY means the database will automatically increment the ID for us.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stockQuantity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }


}