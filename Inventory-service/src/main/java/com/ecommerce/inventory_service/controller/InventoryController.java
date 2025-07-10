package com.ecommerce.inventory_service.controller;



import com.ecommerce.inventory_service.dto.*;
import com.ecommerce.inventory_service.model.Inventory;
import com.ecommerce.inventory_service.model.InventoryHistory;
import com.ecommerce.inventory_service.model.StockReservation;
import com.ecommerce.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Inventory Management
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;



    /**
     * Get inventory information for a specific product
     */
    @GetMapping("/{productId}")
    public ResponseEntity<?> getInventory(@PathVariable Long productId) {
        try {
            Optional<Inventory> inventory = inventoryService.getInventoryByProductId(productId);

            if (inventory.isPresent()) {
                Inventory inv = inventory.get();
                InventoryResponse response = mapToInventoryResponse(inv);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve inventory: " + e.getMessage()));
        }
    }

    /**
     * Get all inventory items
     */
    @GetMapping
    public ResponseEntity<?> getAllInventory(
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles) {

        try {
            // Only admin can view all inventory
            if (!userRoles.contains("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required"));
            }

            List<Inventory> inventories = inventoryService.getAllInventory();
            List<InventoryResponse> responses = inventories.stream()
                    .map(this::mapToInventoryResponse)
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve inventories: " + e.getMessage()));
        }
    }

    /**
     * Create initial inventory for a product (Admin only)
     */
    @PostMapping("/{productId}")
    public ResponseEntity<?> createInventory(
            @PathVariable Long productId,
            @RequestBody StockUpdateRequest request,
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles,
            @RequestHeader(value = "X-Authenticated-User-Username", defaultValue = "system") String username) {

        try {
            if (!userRoles.contains("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required"));
            }

            Inventory inventory = inventoryService.createInventory(productId, request.getQuantity(), username);
            InventoryResponse response = mapToInventoryResponse(inventory);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create inventory: " + e.getMessage()));
        }
    }

    /**
     * Update stock level for a product (Admin only)
     */
    @PutMapping("/{productId}/stock")
    public ResponseEntity<?> updateStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockUpdateRequest request,
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles,
            @RequestHeader(value = "X-Authenticated-User-Username", defaultValue = "system") String username) {

        try {
            if (!userRoles.contains("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required"));
            }

            // Set the performed by field
            request.setPerformedBy(username);

            Inventory inventory = inventoryService.updateStock(productId, request);
            InventoryResponse response = mapToInventoryResponse(inventory);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update stock: " + e.getMessage()));
        }
    }

    /**
     * Adjust stock (add or subtract) for a product (Admin only)
     */
    @PostMapping("/{productId}/adjust")
    public ResponseEntity<?> adjustStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> adjustmentRequest,
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles,
            @RequestHeader(value = "X-Authenticated-User-Username", defaultValue = "system") String username) {

        try {
            if (!userRoles.contains("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required"));
            }

            Integer adjustment = (Integer) adjustmentRequest.get("adjustment");
            String reason = (String) adjustmentRequest.get("reason");

            if (adjustment == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Adjustment value is required"));
            }

            Inventory inventory = inventoryService.adjustStock(productId, adjustment, reason, username);
            InventoryResponse response = mapToInventoryResponse(inventory);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to adjust stock: " + e.getMessage()));
        }
    }

    /**
     * Validate stock availability for a product
     */
    @GetMapping("/{productId}/validate")
    public ResponseEntity<?> validateStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {

        try {
            StockValidationResponse response = inventoryService.validateStock(productId, quantity);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to validate stock: " + e.getMessage()));
        }
    }

    /**
     * Reserve stock for an order
     */
    @PostMapping("/reserve")
    public ResponseEntity<?> reserveStock(
            @Valid @RequestBody StockReservationRequest request,
            @RequestHeader(value = "X-Authenticated-User-Username", defaultValue = "") String username) {

        try {
            // Set user email from header if not provided
            if (request.getUserEmail() == null || request.getUserEmail().isEmpty()) {
                request.setUserEmail(username);
            }

            StockReservation reservation = inventoryService.reserveStock(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reserve stock: " + e.getMessage()));
        }
    }

    /**
     * Confirm a stock reservation (permanently deduct stock)
     */
    @PostMapping("/confirm/{orderId}/{productId}")
    public ResponseEntity<?> confirmReservation(
            @PathVariable String orderId,
            @PathVariable Long productId,
            @RequestHeader(value = "X-Authenticated-User-Username", defaultValue = "") String username) {

        try {
            StockReservation reservation = inventoryService.confirmReservation(orderId, productId, username);
            return ResponseEntity.ok(reservation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to confirm reservation: " + e.getMessage()));
        }
    }

    /**
     * Release a stock reservation (return stock to available)
     */
    @PostMapping("/release/{orderId}/{productId}")
    public ResponseEntity<?> releaseReservation(
            @PathVariable String orderId,
            @PathVariable Long productId,
            @RequestHeader(value = "X-Authenticated-User-Username", defaultValue = "") String username) {

        try {
            StockReservation reservation = inventoryService.releaseReservation(orderId, productId, username);
            return ResponseEntity.ok(reservation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to release reservation: " + e.getMessage()));
        }
    }

    /**
     * Get reservations for an order
     */
    @GetMapping("/reservations/{orderId}")
    public ResponseEntity<?> getReservationsByOrder(@PathVariable String orderId) {
        try {
            List<StockReservation> reservations = inventoryService.getReservationsByOrderId(orderId);
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve reservations: " + e.getMessage()));
        }
    }

    /**
     * Get low stock items (Admin only)
     */
    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStockItems(
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles) {

        try {
            if (!userRoles.contains("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required"));
            }

            List<Inventory> lowStockItems = inventoryService.getLowStockItems();
            List<InventoryResponse> responses = lowStockItems.stream()
                    .map(this::mapToInventoryResponse)
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve low stock items: " + e.getMessage()));
        }
    }

    /**
     * Get items needing reorder (Admin only)
     */
    @GetMapping("/reorder-needed")
    public ResponseEntity<?> getItemsNeedingReorder(
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles) {

        try {
            if (!userRoles.contains("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required"));
            }

            List<Inventory> reorderItems = inventoryService.getItemsNeedingReorder();
            List<InventoryResponse> responses = reorderItems.stream()
                    .map(this::mapToInventoryResponse)
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve reorder items: " + e.getMessage()));
        }
    }

    /**
     * Get inventory history for a product (Admin only)
     */
    @GetMapping("/{productId}/history")
    public ResponseEntity<?> getInventoryHistory(
            @PathVariable Long productId,
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles) {

        try {
            if (!userRoles.contains("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required"));
            }

            List<InventoryHistory> history = inventoryService.getInventoryHistory(productId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve inventory history: " + e.getMessage()));
        }
    }

    /**
     * Bulk update stock levels (Admin only)
     */
    @PostMapping("/bulk-update")
    public ResponseEntity<?> bulkUpdateStock(
            @Valid @RequestBody BulkStockUpdateRequest request,
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles,
            @RequestHeader(value = "X-Authenticated-User-Username", defaultValue = "system") String username) {

        try {
            if (!userRoles.contains("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required"));
            }

            request.setPerformedBy(username);
            List<Inventory> updated = inventoryService.bulkUpdateStock(request);

            List<InventoryResponse> responses = updated.stream()
                    .map(this::mapToInventoryResponse)
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "message", "Bulk update completed successfully",
                    "updated_count", updated.size(),
                    "inventories", responses
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to perform bulk update: " + e.getMessage()));
        }
    }

    /**
     * Adjust reservation quantity (for cart updates)
     */
    @PutMapping("/adjust/{orderId}/{productId}")
    public ResponseEntity<?> adjustReservationQuantity(
            @PathVariable String orderId,
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> quantityRequest,
            @RequestHeader(value = "X-Authenticated-User-Username", defaultValue = "") String username) {

        try {
            Integer newQuantity = quantityRequest.get("quantity");
            if (newQuantity == null || newQuantity < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Valid quantity is required"));
            }

            StockReservation reservation = inventoryService.adjustReservationQuantity(orderId, productId, newQuantity, username);
            return ResponseEntity.ok(reservation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to adjust reservation: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "inventory-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    /**
     * Helper method to map Inventory to InventoryResponse
     */
    private InventoryResponse mapToInventoryResponse(Inventory inventory) {
        InventoryResponse response = new InventoryResponse();
        response.setId(inventory.getId());
        response.setProductId(inventory.getProductId());
        response.setAvailableQuantity(inventory.getAvailableQuantity());
        response.setReservedQuantity(inventory.getReservedQuantity());
        response.setTotalQuantity(inventory.getTotalQuantity());
        response.setMinStockLevel(inventory.getMinStockLevel());
        response.setMaxStockLevel(inventory.getMaxStockLevel());
        response.setReorderPoint(inventory.getReorderPoint());
        response.setLocation(inventory.getLocation());
        response.setIsActive(inventory.getIsActive());
        response.setIsLowStock(inventory.isLowStock());
        response.setCreatedAt(inventory.getCreatedAt());
        response.setUpdatedAt(inventory.getUpdatedAt());
        return response;
    }
}