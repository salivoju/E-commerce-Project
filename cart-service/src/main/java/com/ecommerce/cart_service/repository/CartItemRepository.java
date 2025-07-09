package com.ecommerce.cart_service.repository;

import com.ecommerce.cart_service.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    void deleteByCartIdAndProductId(Long cartId, Long productId);
}
//public interface CartItemRepository extends JpaRepository<CartItem, Long> {
//    List<CartItem> findByCart_Id(Long cartId);
//    Optional<CartItem> findByCart_IdAndProductId(Long cartId, Long productId);
//    void deleteByCart_IdAndProductId(Long cartId, Long productId);
//
//    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cart.userEmail = :userEmail")
//    Integer getTotalItemsByUser(@Param("userEmail") String userEmail);
//}