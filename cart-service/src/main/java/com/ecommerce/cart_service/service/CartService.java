package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.dto.AddToCartRequest;
import com.ecommerce.cart_service.dto.UpdateCartItemRequest;
import com.ecommerce.cart_service.model.Cart;

public interface CartService {
    Cart getCartByUserEmail(String userEmail);
    Cart addToCart(String userEmail, AddToCartRequest request);
    Cart updateCartItem(String userEmail, Long cartItemId, UpdateCartItemRequest request);
    Cart removeFromCart(String userEmail, Long cartItemId);
    void clearCart(String userEmail);
}