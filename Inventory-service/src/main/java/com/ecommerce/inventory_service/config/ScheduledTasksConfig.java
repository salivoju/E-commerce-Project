package com.ecommerce.inventory_service.config;

import com.ecommerce.inventory_service.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduled tasks for inventory management
 */
@Component
public class ScheduledTasksConfig {

    @Autowired
    private InventoryService inventoryService;

    /**
     * Process expired reservations every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 milliseconds
    public void processExpiredReservations() {
        try {
            System.out.println(">>> Processing expired reservations at: " + LocalDateTime.now());
            inventoryService.processExpiredReservations();
        } catch (Exception e) {
            System.err.println("Error processing expired reservations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log low stock alerts every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3,600,000 milliseconds
    public void checkLowStockAlerts() {
        try {
            var lowStockItems = inventoryService.getLowStockItems();
            if (!lowStockItems.isEmpty()) {
                System.out.println(">>> LOW STOCK ALERT: " + lowStockItems.size() + " items are running low:");
                lowStockItems.forEach(item ->
                        System.out.println("   - Product " + item.getProductId() + ": " +
                                item.getAvailableQuantity() + " available (min: " +
                                item.getMinStockLevel() + ")")
                );
            }
        } catch (Exception e) {
            System.err.println("Error checking low stock alerts: " + e.getMessage());
        }
    }

    /**
     * Log reorder recommendations every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours = 21,600,000 milliseconds
    public void checkReorderRecommendations() {
        try {
            var reorderItems = inventoryService.getItemsNeedingReorder();
            if (!reorderItems.isEmpty()) {
                System.out.println(">>> REORDER RECOMMENDATION: " + reorderItems.size() + " items need reordering:");
                reorderItems.forEach(item ->
                        System.out.println("   - Product " + item.getProductId() + ": " +
                                item.getAvailableQuantity() + " available (reorder point: " +
                                item.getReorderPoint() + ")")
                );
            }
        } catch (Exception e) {
            System.err.println("Error checking reorder recommendations: " + e.getMessage());
        }
    }
}
