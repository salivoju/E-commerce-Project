package com.ecommerce.order_service.client;

import com.ecommerce.inventory_service.dto.StockReservationRequest;
import com.ecommerce.inventory_service.dto.StockValidationResponse;
import com.ecommerce.inventory_service.model.StockReservation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "inventory-service")
public interface InventoryServiceClient {

    @PostMapping("/api/v1/inventory/reserve")
    StockReservation reserveStock(@RequestBody StockReservationRequest request);

    @PostMapping("/api/v1/inventory/confirm/{orderId}/{productId}")
    StockReservation confirmReservation(@PathVariable String orderId,
                                        @PathVariable Long productId,
                                        @RequestHeader("X-Authenticated-User-Username") String userEmail);

    @PostMapping("/api/v1/inventory/release/{orderId}/{productId}")
    StockReservation releaseReservation(@PathVariable String orderId,
                                        @PathVariable Long productId,
                                        @RequestHeader("X-Authenticated-User-Username") String userEmail);

    @GetMapping("/api/v1/inventory/{productId}/validate")
    StockValidationResponse validateStock(@PathVariable Long productId,
                                          @RequestParam Integer quantity);

    /**
     * Adjust reservation quantity (when cart item quantity changes)
     * This is the method you were trying to use!
     */
    @PutMapping("/api/v1/inventory/reservation/{orderId}/{productId}/adjust")
    StockReservation adjustReservationQuantity(@PathVariable("orderId") String orderId,
                                               @PathVariable("productId") Long productId,
                                               @RequestParam("newQuantity") Integer newQuantity,
                                               @RequestHeader("X-Authenticated-User-Username") String username);
}
