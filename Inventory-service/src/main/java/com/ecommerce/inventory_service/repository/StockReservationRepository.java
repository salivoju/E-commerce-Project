package com.ecommerce.inventory_service.repository;

import com.ecommerce.inventory_service.model.StockReservation;
import com.ecommerce.inventory_service.model.StockReservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrderId(String orderId);

    List<StockReservation> findByProductId(Long productId);

    List<StockReservation> findByUserEmail(String userEmail);

    List<StockReservation> findByStatus(ReservationStatus status);

    List<StockReservation> findByProductIdAndStatus(Long productId, ReservationStatus status);

    Optional<StockReservation> findByOrderIdAndProductId(String orderId, Long productId);

    List<StockReservation> findByOrderIdAndProductIdAndStatus(String orderId, Long productId, ReservationStatus status);

    // Find expired reservations
    @Query("SELECT sr FROM StockReservation sr WHERE sr.expiresAt < :currentTime AND sr.status = 'ACTIVE'")
    List<StockReservation> findExpiredReservations(@Param("currentTime") LocalDateTime currentTime);

    // Find reservations expiring soon
    @Query("SELECT sr FROM StockReservation sr WHERE sr.expiresAt BETWEEN :currentTime AND :warningTime AND sr.status = 'ACTIVE'")
    List<StockReservation> findReservationsExpiringSoon(@Param("currentTime") LocalDateTime currentTime, @Param("warningTime") LocalDateTime warningTime);

    // Get total reserved quantity for a product
    @Query("SELECT COALESCE(SUM(sr.quantity), 0) FROM StockReservation sr WHERE sr.productId = :productId AND sr.status = 'ACTIVE'")
    Integer getTotalReservedQuantity(@Param("productId") Long productId);

    // Find active reservations for a product
    @Query("SELECT sr FROM StockReservation sr WHERE sr.productId = :productId AND sr.status = 'ACTIVE' AND sr.expiresAt > :currentTime")
    List<StockReservation> findActiveReservationsForProduct(@Param("productId") Long productId, @Param("currentTime") LocalDateTime currentTime);

    void deleteByOrderId(String orderId);
}
