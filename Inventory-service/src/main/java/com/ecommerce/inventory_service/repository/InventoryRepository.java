package com.ecommerce.inventory_service.repository;


import com.ecommerce.inventory_service.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    List<Inventory> findByProductIdIn(List<Long> productIds);

    // Find low stock items
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.minStockLevel AND i.isActive = true")
    List<Inventory> findLowStockItems();

    // Find items that need reordering
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.reorderPoint AND i.isActive = true")
    List<Inventory> findItemsNeedingReorder();

    // Find by location
    List<Inventory> findByLocationAndIsActive(String location, Boolean isActive);

    // Find active inventory items
    List<Inventory> findByIsActive(Boolean isActive);

    // Find items with available stock
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity > 0 AND i.isActive = true")
    List<Inventory> findItemsWithAvailableStock();

    // Check if product exists and has sufficient stock
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId AND i.availableQuantity >= :quantity AND i.isActive = true")
    Optional<Inventory> findByProductIdWithSufficientStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    boolean existsByProductId(Long productId);
}