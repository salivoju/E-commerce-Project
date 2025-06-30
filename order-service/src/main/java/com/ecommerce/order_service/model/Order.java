package com.ecommerce.order_service.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String userEmail;        // Who placed the order
    @Enumerated(EnumType.STRING)
    OrderStatus status;      // PENDING, CONFIRMED, SHIPPED, DELIVERED
    BigDecimal totalAmount;  // Calculated total
    LocalDateTime createdAt;
    @OneToMany(mappedBy = "order",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    List<OrderItem> items;   // One-to-many relationship


}
