package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.CreateOrderRequest;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    /**
     * Create a new order for a user
     * @param request Order creation request with items
     * @param userEmail Email of the user placing the order
     * @return Created order
     */
    Order createOrder(CreateOrderRequest request, String userEmail);

    /**
     * Get all orders for a specific user
     * @param userEmail User's email
     * @return List of orders
     */
    List<Order> getOrdersByUser(String userEmail);

    /**
     * Get order by ID
     * @param orderId Order ID
     * @return Optional order
     */
    Optional<Order> getOrderById(Long orderId);

    /**
     * Get all orders (admin function)
     * @return List of all orders
     */
    List<Order> getAllOrders();

    /**
     * Update order status
     * @param orderId Order ID
     * @param status New status
     * @return Updated order
     */
    Order updateOrderStatus(Long orderId, OrderStatus status);

    /**
     * Get orders by status
     * @param status Order status
     * @return List of orders with specified status
     */
    List<Order> getOrdersByStatus(OrderStatus status);
}