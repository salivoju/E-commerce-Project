package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.client.ProductServiceClient;
import com.ecommerce.inventory_service.dto.ProductResponse;
import com.ecommerce.inventory_service.model.Inventory;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service to sync inventory with product catalog
 */
@Service
public class InventorySyncService {

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryService inventoryService;

    /**
     * Sync inventory for a specific product
     */
    @Transactional
    public void syncProductInventory(Long productId) {
        try {
            // Get product info from product service
            ProductResponse productInfo = productServiceClient.getProductById(productId);

            // Check if inventory exists
            Optional<Inventory> existingInventory = inventoryRepository.findByProductId(productId);

            if (existingInventory.isEmpty()) {
                // Create new inventory record
                System.out.println("Creating new inventory for product: " + productId);
                inventoryService.createInventory(productId, 0, "SYSTEM_SYNC");
            } else {
                System.out.println("Inventory already exists for product: " + productId);
            }
        } catch (Exception e) {
            System.err.println("Failed to sync inventory for product " + productId + ": " + e.getMessage());
        }
    }

    /**
     * Validate that a product exists before inventory operations
     */
    public boolean validateProductExists(Long productId) {
        try {
            ProductResponse productInfo = productServiceClient.getProductById(productId);
            return productInfo != null && productInfo.getId() != null;
        } catch (Exception e) {
            System.err.println("Product validation failed for ID " + productId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Get product name for inventory reporting
     */
    public String getProductName(Long productId) {
        try {
            ProductResponse productInfo = productServiceClient.getProductById(productId);
            return productInfo != null ? productInfo.getName() : "Unknown Product";
        } catch (Exception e) {
            System.err.println("Failed to get product name for ID " + productId + ": " + e.getMessage());
            return "Unknown Product";
        }
    }
}
