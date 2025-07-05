package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {

    List<OrderItem> findByOrder_Id(Long orderId);

    // Get all items for a specific order (by Order object)
    List<OrderItem> findByOrder(Order order);

    // Get all order items containing a specific product
    List<OrderItem> findByProductId(Long productId);

    // Find specific item in specific order
    Optional<OrderItem> findByOrder_IdAndProductId(Long orderId, Long productId);

}
