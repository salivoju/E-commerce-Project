package com.ecommerce.order_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * OrderItem represents individual items within an order.
 * Each OrderItem belongs to exactly one Order and references a Product from product-service.
 */
@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many OrderItems belong to one Order
     * Using @JoinColumn to specify the foreign key column name
     * FetchType.LAZY for performance - only load Order when needed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Reference to Product in product-service
     * We store the ID to reference the external service
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * Cached product name for performance and data consistency
     * This ensures we keep the product name even if it changes in product-service
     * or if the product gets deleted
     */
    @Column(name = "product_name", nullable = false)
    private String productName;

    /**
     * Quantity of this product ordered
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Price per unit at the time of order
     * Important: We store the price at time of purchase, not current price
     * This ensures order history remains accurate even if prices change
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Total price for this line item (quantity * unitPrice)
     * Could be calculated, but storing it ensures data consistency
     * and improves query performance
     */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    /**
     * Convenience method to calculate total price
     * Used during order creation
     */
    public void calculateTotalPrice() {
        if (quantity != null && unitPrice != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /**
     * Builder-style method to set order relationship
     * Useful for maintaining bidirectional relationship
     */
    public OrderItem setOrder(Order order) {
        this.order = order;
        return this;
    }

    /**
     * Override toString to avoid circular reference with Order
     * (since Order has List<OrderItem> and OrderItem has Order)
     */
    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                '}';
    }
}