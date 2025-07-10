package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.*;
import com.ecommerce.inventory_service.model.Inventory;
import com.ecommerce.inventory_service.model.InventoryHistory;
import com.ecommerce.inventory_service.model.StockReservation;

import java.util.List;
import java.util.Optional;

public interface InventoryService {

    // Core Inventory Management
    Optional<Inventory> getInventoryByProductId(Long productId);
    List<Inventory> getAllInventory();
    Inventory createInventory(Long productId, Integer initialStock, String performedBy);
    Inventory updateStock(Long productId, StockUpdateRequest request);

    // Stock Validation
    StockValidationResponse validateStock(Long productId, Integer quantity);
    List<StockValidationResponse> validateMultipleStock(List<Long> productIds, List<Integer> quantities);

    // Stock Reservation Management
    StockReservation reserveStock(StockReservationRequest request);
    StockReservation confirmReservation(String orderId, Long productId, String userEmail);
    StockReservation releaseReservation(String orderId, Long productId, String userEmail);
    List<StockReservation> getReservationsByOrderId(String orderId);
    List<StockReservation> getActiveReservationsForProduct(Long productId);
    StockReservation adjustReservationQuantity(String orderId, Long productId, Integer newQuantity, String userEmail);

    // Bulk Operations
    List<Inventory> bulkUpdateStock(BulkStockUpdateRequest request);
    void processExpiredReservations();

    // Low Stock Management
    List<Inventory> getLowStockItems();
    List<Inventory> getItemsNeedingReorder();

    // Inventory History
    List<InventoryHistory> getInventoryHistory(Long productId);
    List<InventoryHistory> getRecentInventoryHistory(int days);

    // Admin Operations
    Inventory adjustStock(Long productId, Integer adjustment, String reason, String performedBy);
    void deactivateInventory(Long productId);
    void activateInventory(Long productId);

    // Integration Methods (for other services)
    boolean checkAvailability(Long productId, Integer quantity);
    void updateMinStockLevel(Long productId, Integer minLevel);
    void syncWithProductCatalog();
}