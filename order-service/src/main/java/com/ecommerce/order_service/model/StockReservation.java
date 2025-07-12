package com.ecommerce.order_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks individual stock reservations for orders
 * This allows us to hold stock temporarily while orders are being processed
 */
@Entity
@Table(name = "stock_reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the product being reserved
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * Quantity reserved
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Reference to the order this reservation is for
     */
    @Column(name = "order_id", nullable = false)
    private String orderId;

    /**
     * User email who made the reservation
     */
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    /**
     * Current status of the reservation
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    /**
     * When the reservation was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When the reservation expires (if not confirmed or released)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * When the reservation was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * When the reservation was completed/released
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Additional notes
     */
    @Column(name = "notes")
    private String notes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Default expiration: 30 minutes from creation
        if (expiresAt == null) {
            expiresAt = createdAt.plusMinutes(30);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (status == ReservationStatus.CONFIRMED ||
                status == ReservationStatus.RELEASED ||
                status == ReservationStatus.EXPIRED) {
            completedAt = LocalDateTime.now();
        }
    }

    /**
     * Check if the reservation has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) && status == ReservationStatus.ACTIVE;
    }

    /**
     * Check if the reservation is still active
     */
    public boolean isActive() {
        return status == ReservationStatus.ACTIVE && !isExpired();
    }

    /**
     * Mark reservation as confirmed (stock will be deducted)
     */
    public void confirm() {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Can only confirm active reservations");
        }
        if (isExpired()) {
            throw new IllegalStateException("Cannot confirm expired reservation");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    /**
     * Release the reservation (return stock to available)
     */
    public void release() {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Can only release active reservations");
        }
        this.status = ReservationStatus.RELEASED;
    }

    /**
     * Mark reservation as expired
     */
    public void expire() {
        if (status == ReservationStatus.ACTIVE) {
            this.status = ReservationStatus.EXPIRED;
        }
    }

    /**
     * Extend the reservation expiry time
     */
    public void extendExpiry(int minutesToAdd) {
        if (status == ReservationStatus.ACTIVE) {
            this.expiresAt = this.expiresAt.plusMinutes(minutesToAdd);
        }
    }

    /**
     * Reservation status types
     */
    public enum ReservationStatus {
        ACTIVE,     // Reservation is active and holding stock
        CONFIRMED,  // Reservation confirmed, stock permanently deducted
        RELEASED,   // Reservation released, stock returned to available
        EXPIRED     // Reservation expired, stock should be returned
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
