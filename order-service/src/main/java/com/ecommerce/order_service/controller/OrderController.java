package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.CreateOrderRequest;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderStatus;
import com.ecommerce.order_service.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Order operations
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Create a new order
     * @param request Order creation request
     * @param userEmail User email from JWT (passed by API Gateway)
     * @return Created order
     */
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader("X-Authenticated-User-Username") String userEmail) {

        try {
            System.out.println(">>> Creating order for user: " + userEmail);
            System.out.println(">>> Order items count: " + request.getItems().size());

            Order createdOrder = orderService.createOrder(request, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);

        } catch (RuntimeException e) {
            System.err.println(">>> Error creating order: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println(">>> Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Get all orders for the authenticated user
     * @param userEmail User email from JWT
     * @return List of user's orders
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(
            @RequestHeader("X-Authenticated-User-Username") String userEmail) {

        List<Order> orders = orderService.getOrdersByUser(userEmail);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get order by ID (user can only see their own orders)
     * @param orderId Order ID
     * @param userEmail User email from JWT
     * @return Order details
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(
            @PathVariable Long orderId,
            @RequestHeader("X-Authenticated-User-Username") String userEmail,
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles) {

        Optional<Order> orderOptional = orderService.getOrderById(orderId);

        if (orderOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = orderOptional.get();

        // Users can only see their own orders, unless they're admin
        if (!userRoles.contains("ROLE_ADMIN") && !order.getUserEmail().equals(userEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only view your own orders"));
        }

        return ResponseEntity.ok(order);
    }

    /**
     * Get all orders (Admin only)
     * @param userRoles User roles from JWT
     * @return List of all orders
     */
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllOrders(
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles) {

        if (!userRoles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required"));
        }

        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Update order status (Admin only)
     * @param orderId Order ID
     * @param statusRequest New status
     * @param userRoles User roles from JWT
     * @return Updated order
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> statusRequest,
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles) {

        if (!userRoles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required"));
        }

        try {
            String statusString = statusRequest.get("status");
            OrderStatus status = OrderStatus.valueOf(statusString.toUpperCase());

            Order updatedOrder = orderService.updateOrderStatus(orderId, status);
            return ResponseEntity.ok(updatedOrder);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid status. Valid values: PENDING, CONFIRMED, SHIPPED, DELIVERED"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get orders by status (Admin only)
     * @param status Order status
     * @param userRoles User roles from JWT
     * @return List of orders with specified status
     */
    @GetMapping("/admin/status/{status}")
    public ResponseEntity<?> getOrdersByStatus(
            @PathVariable String status,
            @RequestHeader(value = "X-Authenticated-User-Roles", defaultValue = "") String userRoles) {

        if (!userRoles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required"));
        }

        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            List<Order> orders = orderService.getOrdersByStatus(orderStatus);
            return ResponseEntity.ok(orders);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid status. Valid values: PENDING, CONFIRMED, SHIPPED, DELIVERED"));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "order-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}