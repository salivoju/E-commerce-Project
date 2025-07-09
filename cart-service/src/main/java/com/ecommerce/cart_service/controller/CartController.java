package com.ecommerce.cart_service.controller;

import com.ecommerce.cart_service.dto.AddToCartRequest;
import com.ecommerce.cart_service.dto.UpdateCartItemRequest;
import com.ecommerce.cart_service.model.Cart;
import com.ecommerce.cart_service.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<Cart> getCart(
            @RequestHeader("X-Authenticated-User-Username") String userEmail) {

        try {
            Cart cart = cartService.getCartByUserEmail(userEmail);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/items")
    public ResponseEntity<?> addToCart(
            @RequestHeader("X-Authenticated-User-Username") String userEmail,
            @RequestBody AddToCartRequest request) {

        try {
            Cart cart = cartService.addToCart(userEmail, request);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<?> updateCartItem(
            @RequestHeader("X-Authenticated-User-Username") String userEmail,
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartItemRequest request) {

        try {
            Cart cart = cartService.updateCartItem(userEmail, cartItemId, request);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<?> removeFromCart(
            @RequestHeader("X-Authenticated-User-Username") String userEmail,
            @PathVariable Long cartItemId) {

        try {
            Cart cart = cartService.removeFromCart(userEmail, cartItemId);
            return ResponseEntity.ok(Map.of(
                    "message", "Item removed from cart successfully",
                    "cart", cart
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearCart(
            @RequestHeader("X-Authenticated-User-Username") String userEmail) {

        try {
            cartService.clearCart(userEmail);
            return ResponseEntity.ok(Map.of("message", "Cart cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "cart-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}