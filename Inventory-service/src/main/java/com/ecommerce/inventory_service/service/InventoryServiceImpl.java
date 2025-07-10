package com.ecommerce.inventory_service.service;


import com.ecommerce.inventory_service.dto.*;
import com.ecommerce.inventory_service.model.Inventory;
import com.ecommerce.inventory_service.model.InventoryHistory;
import com.ecommerce.inventory_service.model.InventoryHistory.OperationType;
import com.ecommerce.inventory_service.model.StockReservation;
import com.ecommerce.inventory_service.model.StockReservation.ReservationStatus;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import com.ecommerce.inventory_service.repository.InventoryHistoryRepository;
import com.ecommerce.inventory_service.repository.StockReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryHistoryRepository historyRepository;

    @Autowired
    private StockReservationRepository reservationRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Inventory> getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findByIsActive(true);
    }

    @Override
    @Transactional
    public Inventory createInventory(Long productId, Integer initialStock, String performedBy) {
        // Check if inventory already exists
        if (inventoryRepository.existsByProductId(productId)) {
            throw new RuntimeException("Inventory already exists for product ID: " + productId);
        }

        // Create new inventory
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setAvailableQuantity(initialStock);
        inventory.setReservedQuantity(0);
        inventory.setIsActive(true);

        Inventory saved = inventoryRepository.save(inventory);

        // Record history
        recordInventoryHistory(productId, OperationType.INITIAL_STOCK, initialStock,
                0, initialStock, null, "INITIAL_SETUP",
                performedBy, "Initial inventory setup", "MAIN_WAREHOUSE");

        return saved;
    }

    @Override
    @Transactional
    public Inventory updateStock(Long productId, StockUpdateRequest request) {
        Inventory inventory = getOrCreateInventory(productId);

        Integer oldQuantity = inventory.getAvailableQuantity();
        inventory.setAvailableQuantity(request.getQuantity());

        Inventory saved = inventoryRepository.save(inventory);

        // Record history
        recordInventoryHistory(productId, OperationType.STOCK_IN,
                request.getQuantity() - oldQuantity,
                oldQuantity, request.getQuantity(),
                null, "MANUAL_UPDATE",
                request.getPerformedBy(),
                request.getNotes(),
                request.getLocation());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public StockValidationResponse validateStock(Long productId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);

        if (inventoryOpt.isEmpty()) {
            return new StockValidationResponse(productId, false, 0, quantity,
                    "Product not found in inventory");
        }

        Inventory inventory = inventoryOpt.get();
        boolean isAvailable = inventory.getAvailableQuantity() >= quantity;
        String message = isAvailable ? "Stock available" :
                "Insufficient stock. Available: " + inventory.getAvailableQuantity();

        return new StockValidationResponse(productId, isAvailable,
                inventory.getAvailableQuantity(),
                quantity, message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockValidationResponse> validateMultipleStock(List<Long> productIds, List<Integer> quantities) {
        List<StockValidationResponse> responses = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Integer quantity = quantities.get(i);
            responses.add(validateStock(productId, quantity));
        }

        return responses;
    }

    @Override
    @Transactional
    public StockReservation reserveStock(StockReservationRequest request) {
        Inventory inventory = getOrCreateInventory(request.getProductId());

        // Check if we can reserve the stock
        if (!inventory.canReserve(request.getQuantity())) {
            throw new RuntimeException("Cannot reserve " + request.getQuantity() +
                    " items for product " + request.getProductId() +
                    ". Available: " + inventory.getAvailableQuantity());
        }

        // Reserve the stock
        inventory.reserveStock(request.getQuantity());
        inventoryRepository.save(inventory);

        // Create reservation record
        StockReservation reservation = new StockReservation();
        reservation.setProductId(request.getProductId());
        reservation.setQuantity(request.getQuantity());
        reservation.setOrderId(request.getOrderId());
        reservation.setUserEmail(request.getUserEmail());
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(request.getExpirationMinutes()));
        reservation.setNotes(request.getNotes());

        StockReservation saved = reservationRepository.save(reservation);

        // Record history
        recordInventoryHistory(request.getProductId(), OperationType.STOCK_RESERVED,
                -request.getQuantity(),
                inventory.getAvailableQuantity() + request.getQuantity(),
                inventory.getAvailableQuantity(),
                request.getOrderId(), "ORDER_RESERVATION",
                request.getUserEmail(),
                "Stock reserved for order: " + request.getOrderId(),
                inventory.getLocation());

        return saved;
    }

    @Override
    @Transactional
    public StockReservation confirmReservation(String orderId, Long productId, String userEmail) {
        StockReservation reservation = reservationRepository.findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new RuntimeException("Reservation not found for order: " + orderId +
                        " and product: " + productId));

        if (!reservation.isActive()) {
            throw new RuntimeException("Reservation is not active. Status: " + reservation.getStatus());
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));

        // Confirm the reservation (remove from reserved permanently)
        inventory.confirmStockUsage(reservation.getQuantity());
        inventoryRepository.save(inventory);

        // Update reservation status
        reservation.confirm();
        StockReservation saved = reservationRepository.save(reservation);

        // Record history
        recordInventoryHistory(productId, OperationType.STOCK_CONFIRMED,
                -reservation.getQuantity(),
                inventory.getReservedQuantity() + reservation.getQuantity(),
                inventory.getReservedQuantity(),
                orderId, "ORDER_CONFIRMED",
                userEmail,
                "Stock confirmed for order: " + orderId,
                inventory.getLocation());

        return saved;
    }

    @Override
    @Transactional
    public StockReservation releaseReservation(String orderId, Long productId, String userEmail) {
        StockReservation reservation = reservationRepository.findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new RuntimeException("Reservation not found for order: " + orderId +
                        " and product: " + productId));

        if (!reservation.isActive()) {
            throw new RuntimeException("Reservation is not active. Status: " + reservation.getStatus());
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));

        // Release the reservation (return to available stock)
        inventory.releaseReservedStock(reservation.getQuantity());
        inventoryRepository.save(inventory);

        // Update reservation status
        reservation.release();
        StockReservation saved = reservationRepository.save(reservation);

        // Record history
        recordInventoryHistory(productId, OperationType.STOCK_RELEASED,
                reservation.getQuantity(),
                inventory.getAvailableQuantity() - reservation.getQuantity(),
                inventory.getAvailableQuantity(),
                orderId, "ORDER_CANCELLED",
                userEmail,
                "Stock released from order: " + orderId,
                inventory.getLocation());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockReservation> getReservationsByOrderId(String orderId) {
        return reservationRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockReservation> getActiveReservationsForProduct(Long productId) {
        return reservationRepository.findActiveReservationsForProduct(productId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public List<Inventory> bulkUpdateStock(BulkStockUpdateRequest request) {
        List<Inventory> updatedInventories = new ArrayList<>();

        for (BulkStockUpdateRequest.BulkStockItem item : request.getItems()) {
            StockUpdateRequest stockUpdateRequest = new StockUpdateRequest();
            stockUpdateRequest.setQuantity(item.getQuantity());
            stockUpdateRequest.setNotes(item.getNotes());
            stockUpdateRequest.setPerformedBy(request.getPerformedBy());

            Inventory updated = updateStock(item.getProductId(), stockUpdateRequest);
            updatedInventories.add(updated);
        }

        return updatedInventories;
    }

    @Override
    @Transactional
    public void processExpiredReservations() {
        List<StockReservation> expiredReservations = reservationRepository
                .findExpiredReservations(LocalDateTime.now());

        for (StockReservation reservation : expiredReservations) {
            try {
                // Release the stock back to available
                Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                        .orElse(null);

                if (inventory != null) {
                    inventory.releaseReservedStock(reservation.getQuantity());
                    inventoryRepository.save(inventory);
                }

                // Mark reservation as expired
                reservation.expire();
                reservationRepository.save(reservation);

                // Record history
                recordInventoryHistory(reservation.getProductId(), OperationType.STOCK_RELEASED,
                        reservation.getQuantity(),
                        inventory != null ? inventory.getAvailableQuantity() - reservation.getQuantity() : 0,
                        inventory != null ? inventory.getAvailableQuantity() : 0,
                        reservation.getOrderId(), "RESERVATION_EXPIRED",
                        "SYSTEM",
                        "Expired reservation released",
                        inventory != null ? inventory.getLocation() : "UNKNOWN");

                System.out.println("Processed expired reservation: " + reservation.getId() +
                        " for order: " + reservation.getOrderId());
            } catch (Exception e) {
                System.err.println("Error processing expired reservation " + reservation.getId() + ": " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> getLowStockItems() {
        return inventoryRepository.findLowStockItems();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> getItemsNeedingReorder() {
        return inventoryRepository.findItemsNeedingReorder();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryHistory> getInventoryHistory(Long productId) {
        return historyRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryHistory> getRecentInventoryHistory(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return historyRepository.findRecentHistory(fromDate);
    }

    @Override
    @Transactional
    public Inventory adjustStock(Long productId, Integer adjustment, String reason, String performedBy) {
        Inventory inventory = getOrCreateInventory(productId);

        Integer oldQuantity = inventory.getAvailableQuantity();
        Integer newQuantity = oldQuantity + adjustment;

        if (newQuantity < 0) {
            throw new RuntimeException("Stock adjustment would result in negative stock. " +
                    "Current: " + oldQuantity + ", Adjustment: " + adjustment);
        }

        inventory.setAvailableQuantity(newQuantity);
        Inventory saved = inventoryRepository.save(inventory);

        // Record history
        OperationType operationType = adjustment > 0 ? OperationType.ADJUSTMENT_POSITIVE : OperationType.ADJUSTMENT_NEGATIVE;
        recordInventoryHistory(productId, operationType, adjustment,
                oldQuantity, newQuantity,
                null, "MANUAL_ADJUSTMENT",
                performedBy, reason, inventory.getLocation());

        return saved;
    }

    @Override
    @Transactional
    public void deactivateInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));

        inventory.setIsActive(false);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void activateInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));

        inventory.setIsActive(true);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkAvailability(Long productId, Integer quantity) {
        return inventoryRepository.findByProductIdWithSufficientStock(productId, quantity).isPresent();
    }

    @Override
    @Transactional
    public void updateMinStockLevel(Long productId, Integer minLevel) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));

        inventory.setMinStockLevel(minLevel);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void syncWithProductCatalog() {
        // This method would sync with product catalog service
        // Implementation depends on your product catalog service API
        System.out.println("Syncing with product catalog...");
        // TODO: Implement product catalog sync
    }

    // Helper methods

    private Inventory getOrCreateInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseGet(() -> {
                    Inventory newInventory = new Inventory();
                    newInventory.setProductId(productId);
                    newInventory.setAvailableQuantity(0);
                    newInventory.setReservedQuantity(0);
                    newInventory.setIsActive(true);
                    return inventoryRepository.save(newInventory);
                });
    }

    private void recordInventoryHistory(Long productId, OperationType operationType,
                                        Integer quantityChange, Integer quantityBefore,
                                        Integer quantityAfter, String referenceId,
                                        String referenceType, String performedBy,
                                        String notes, String location) {
        InventoryHistory history = new InventoryHistory();
        history.setProductId(productId);
        history.setOperationType(operationType);
        history.setQuantityChange(quantityChange);
        history.setQuantityBefore(quantityBefore);
        history.setQuantityAfter(quantityAfter);
        history.setReferenceId(referenceId);
        history.setReferenceType(referenceType);
        history.setPerformedBy(performedBy);
        history.setNotes(notes);
        history.setLocation(location);

        historyRepository.save(history);
    }
}