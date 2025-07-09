package com.ecommerce.cart_service.repository;

import com.ecommerce.cart_service.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserEmail(String userEmail);

    // Find all carts for a user (for cleanup if duplicates exist)
    List<Cart> findAllByUserEmail(String userEmail);

    void deleteByUserEmail(String userEmail);

    // Check if cart exists for user
    boolean existsByUserEmail(String userEmail);

    // Custom query to get cart with items
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userEmail = :userEmail")
    Optional<Cart> findByUserEmailWithItems(@Param("userEmail") String userEmail);
}