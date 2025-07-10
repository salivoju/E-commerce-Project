package com.ecommerce.inventory_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks all inventory movements and changes for auditing purposes
 */
@Entity
@Table(name = "inventory_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the product whose inventory changed
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * Type of operation performed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    /**
     * Quantity involved in the operation (positive for additions, negative for reductions)
     */
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    /**
     * Stock level before the operation
     */
    @Column(name = "quantity_before")
    private Integer quantityBefore;

    /**
     * Stock level after the operation
     */
    @Column(name = "quantity_after")
    private Integer quantityAfter;

    /**
     * Reference ID for the operation (e.g., order ID, reservation ID)
     */
    @Column(name = "reference_id")
    private String referenceId;

    /**
     * Reference type (e.g., ORDER, MANUAL_ADJUSTMENT, RESERVATION)
     */
    @Column(name = "reference_type")
    private String referenceType;

    /**
     * User or system that performed the operation
     */
    @Column(name = "performed_by")
    private String performedBy;

    /**
     * Additional notes or reason for the change
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Location where the operation occurred
     */
    @Column(name = "location")
    private String location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Types of inventory operations
     */
    public enum OperationType {
        STOCK_IN,           // New stock received
        STOCK_OUT,          // Stock removed/sold
        STOCK_RESERVED,     // Stock reserved for order
        STOCK_RELEASED,     // Reserved stock released back
        STOCK_CONFIRMED,    // Reserved stock confirmed/used
        ADJUSTMENT_POSITIVE, // Manual positive adjustment
        ADJUSTMENT_NEGATIVE, // Manual negative adjustment
        TRANSFER_IN,        // Stock transferred in from another location
        TRANSFER_OUT,       // Stock transferred out to another location
        DAMAGED,            // Stock marked as damaged/lost
        RETURNED,           // Stock returned from customer
        REORDER,           // Reorder operation
        INITIAL_STOCK      // Initial stock setup
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

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Integer getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(Integer quantityChange) {
        this.quantityChange = quantityChange;
    }

    public Integer getQuantityBefore() {
        return quantityBefore;
    }

    public void setQuantityBefore(Integer quantityBefore) {
        this.quantityBefore = quantityBefore;
    }

    public Integer getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}