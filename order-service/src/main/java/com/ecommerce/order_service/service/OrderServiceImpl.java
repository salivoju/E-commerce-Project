package com.ecommerce.order_service.service;

import com.ecommerce.order_service.client.ProductServiceClient;
import com.ecommerce.order_service.dto.CreateOrderRequest;
import com.ecommerce.order_service.dto.ProductResponse;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderItem;
import com.ecommerce.order_service.model.OrderStatus;
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

    @Transactional
    @Override
    public Order createOrder(CreateOrderRequest request, String userEmail) {
        // Create new order
        Order order = new Order();
        order.setUserEmail(userEmail);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Process each order item
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            // Get product details from Product Service
            ProductResponse product;
            try {
                product = productServiceClient.getProductById(itemRequest.getProductId());
            } catch (Exception e) {
                throw new RuntimeException("Product with ID " + itemRequest.getProductId() + " not found or service unavailable");
            }

            // Validate stock availability
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getStockQuantity() + ", Requested: " + itemRequest.getQuantity());
            }

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.calculateTotalPrice(); // This sets totalPrice = unitPrice * quantity

            // Add to order
            order.getItems().add(orderItem);

            // Add to total amount
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }

        // Set total amount
        order.setTotalAmount(totalAmount);

        // Save order (this will cascade save the order items)
        return orderRepository.save(order);
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

    @Transactional
    @Override
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Optional<Order> orderOptional = orderRepository.findById(orderId);
        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            order.setStatus(status);
            return orderRepository.save(order);
        } else {
            throw new RuntimeException("Order with ID " + orderId + " not found");
        }
    }

    @Override
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
}