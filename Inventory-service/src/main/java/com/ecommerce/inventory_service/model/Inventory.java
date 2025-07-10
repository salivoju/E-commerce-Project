package com.ecommerce.inventory_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Main Inventory entity that tracks current stock levels for products
 */
@Entity
@Table(name = "inventory",
        uniqueConstraints = @UniqueConstraint(columnNames = "product_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to Product ID from product-catalog-service
     */
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    /**
     * Current available stock (not reserved)
     */
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity = 0;

    /**
     * Stock currently reserved for pending orders
     */
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    /**
     * Total physical stock (available + reserved)
     */
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity = 0;

    /**
     * Minimum stock level threshold for low stock alerts
     */
    @Column(name = "min_stock_level", nullable = false)
    private Integer minStockLevel = 10;

    /**
     * Maximum stock level for reorder planning
     */
    @Column(name = "max_stock_level")
    private Integer maxStockLevel = 1000;

    /**
     * Reorder point - when to reorder stock
     */
    @Column(name = "reorder_point")
    private Integer reorderPoint = 20;

    /**
     * Location/warehouse where stock is stored
     */
    @Column(name = "location")
    private String location = "MAIN_WAREHOUSE";

    /**
     * Is this product currently being tracked?
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        updateTotalQuantity();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateTotalQuantity();
    }

    /**
     * Ensures total quantity is consistent
     */
    private void updateTotalQuantity() {
        this.totalQuantity = this.availableQuantity + this.reservedQuantity;
    }

    /**
     * Check if stock is below minimum level
     */
    public boolean isLowStock() {
        return this.availableQuantity <= this.minStockLevel;
    }

    /**
     * Check if we can reserve the requested quantity
     */
    public boolean canReserve(Integer quantity) {
        return quantity != null && quantity > 0 && this.availableQuantity >= quantity;
    }

    /**
     * Reserve stock (reduce available, increase reserved)
     */
    public void reserveStock(Integer quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Cannot reserve " + quantity +
                    " items. Available: " + this.availableQuantity);
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
        updateTotalQuantity();
    }

    /**
     * Release reserved stock back to available
     */
    public void releaseReservedStock(Integer quantity) {
        if (quantity == null || quantity <= 0 || this.reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release " + quantity +
                    " items. Reserved: " + this.reservedQuantity);
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
        updateTotalQuantity();
    }

    /**
     * Confirm stock usage (remove from reserved permanently)
     */
    public void confirmStockUsage(Integer quantity) {
        if (quantity == null || quantity <= 0 || this.reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot confirm usage of " + quantity +
                    " items. Reserved: " + this.reservedQuantity);
        }
        this.reservedQuantity -= quantity;
        updateTotalQuantity();
    }

    /**
     * Add new stock (increases available quantity)
     */
    public void addStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }
        this.availableQuantity += quantity;
        updateTotalQuantity();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(Integer minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public Integer getMaxStockLevel() {
        return maxStockLevel;
    }

    public void setMaxStockLevel(Integer maxStockLevel) {
        this.maxStockLevel = maxStockLevel;
    }

    public Integer getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(Integer reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}