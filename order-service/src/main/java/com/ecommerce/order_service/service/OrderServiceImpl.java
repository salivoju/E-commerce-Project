package com.ecommerce.order_service.service;

import com.ecommerce.inventory_service.dto.StockReservationRequest;
import com.ecommerce.inventory_service.dto.StockValidationResponse;
import com.ecommerce.order_service.client.InventoryServiceClient;
import com.ecommerce.order_service.client.ProductServiceClient;
import com.ecommerce.order_service.dto.CreateOrderRequest;
import com.ecommerce.order_service.dto.ProductResponse;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderItem;
import com.ecommerce.order_service.model.OrderStatus;
import com.ecommerce.inventory_service.model.StockReservation;
import com.ecommerce.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private InventoryServiceClient inventoryClient;

    @Transactional
    @Override
    public Order createOrder(CreateOrderRequest request, String userEmail) {
        System.out.println(">>> Starting order creation for user: " + userEmail);

        // 1. Validate stock availability first using inventory service
        System.out.println(">>> Step 1: Validating stock availability");
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            StockValidationResponse validation = inventoryClient.validateStock(
                    item.getProductId(), item.getQuantity());

            if (!validation.getIsAvailable()) {
                throw new RuntimeException("Insufficient stock for product " +
                        item.getProductId() + ": " + validation.getMessage());
            }
            System.out.println(">>> Stock validated for product " + item.getProductId() +
                    ": " + item.getQuantity() + " items available");
        }

        // 2. Create initial order to get an ID
        System.out.println(">>> Step 2: Creating initial order");
        Order order = new Order();
        order.setUserEmail(userEmail);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());

        // Save order to get an ID for reservations
        Order savedOrder = orderRepository.save(order);
        String orderId = "ORDER-" + savedOrder.getId();
        System.out.println(">>> Order created with ID: " + orderId);

        // 3. Reserve stock for each item using inventory service
        System.out.println(">>> Step 3: Reserving stock for order items");
        try {
            for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
                StockReservationRequest reservationRequest = new StockReservationRequest();
                reservationRequest.setProductId(item.getProductId());
                reservationRequest.setQuantity(item.getQuantity());
                reservationRequest.setOrderId(orderId);
                reservationRequest.setUserEmail(userEmail);
                reservationRequest.setExpirationMinutes(30); // 30-minute reservation
                reservationRequest.setNotes("Order creation reservation for " + orderId);

                inventoryClient.reserveStock(reservationRequest);
                System.out.println(">>> Reserved " + item.getQuantity() +
                        " units of product " + item.getProductId());
            }
        } catch (Exception e) {
            System.err.println(">>> Stock reservation failed, rolling back order");
            // If reservation fails, we should clean up the order
            orderRepository.delete(savedOrder);
            throw new RuntimeException("Failed to reserve stock: " + e.getMessage());
        }

        // 4. Process order items and build complete order
        System.out.println(">>> Step 4: Processing order items and calculating totals");
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            // Get product details from Product Service
            ProductResponse product;
            try {
                product = productServiceClient.getProductById(itemRequest.getProductId());
            } catch (Exception e) {
                // If product lookup fails after reservation, release reservations
                System.err.println(">>> Product lookup failed, releasing reservations");
                releaseOrderReservations(orderId, savedOrder.getItems(), userEmail);
                orderRepository.delete(savedOrder);
                throw new RuntimeException("Product with ID " + itemRequest.getProductId() +
                        " not found or service unavailable");
            }

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.calculateTotalPrice(); // This sets totalPrice = unitPrice * quantity

            // Add to order
            savedOrder.getItems().add(orderItem);

            // Add to total amount
            totalAmount = totalAmount.add(orderItem.getTotalPrice());

            System.out.println(">>> Added item: " + product.getName() +
                    " x" + itemRequest.getQuantity() +
                    " = $" + orderItem.getTotalPrice());
        }

        // 5. Set total amount and save final order
        savedOrder.setTotalAmount(totalAmount);
        Order finalOrder = orderRepository.save(savedOrder);

        System.out.println(">>> Order creation completed. Total: $" + totalAmount);
        System.out.println(">>> Stock reserved for 30 minutes. Order status: " + finalOrder.getStatus());

        return finalOrder;
    }

    @Override
    public List<Order> getOrdersByUser(String userEmail) {
        return orderRepository.findByUserEmail(userEmail);
    }


    @Override
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String orderIdStr = "ORDER-" + orderId;

        if (status == OrderStatus.CONFIRMED) {
            // Confirm all reservations
            for (OrderItem item : order.getItems()) {
                inventoryClient.confirmReservation(orderIdStr,
                        item.getProductId(), order.getUserEmail());
            }
        } else if (status == OrderStatus.CANCELLED) {
            // Release all reservations
            for (OrderItem item : order.getItems()) {
                inventoryClient.releaseReservation(orderIdStr,
                        item.getProductId(), order.getUserEmail());
            }
        }

        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Override
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
}