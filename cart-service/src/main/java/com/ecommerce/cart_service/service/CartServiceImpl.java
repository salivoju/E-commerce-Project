package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.client.ProductServiceClient;
import com.ecommerce.cart_service.dto.AddToCartRequest;
import com.ecommerce.cart_service.dto.StockReservationRequest;
import com.ecommerce.cart_service.dto.StockValidationResponse;
import com.ecommerce.cart_service.client.InventoryServiceClient;
import com.ecommerce.cart_service.dto.ProductResponse;
import com.ecommerce.cart_service.dto.UpdateCartItemRequest;
import com.ecommerce.cart_service.model.Cart;
import com.ecommerce.cart_service.model.CartItem;
import com.ecommerce.cart_service.repository.CartRepository;
import com.ecommerce.cart_service.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private InventoryServiceClient inventoryClient;

    @Override
    @Transactional(readOnly = true)
    public Cart getCartByUserEmail(String userEmail) {
        // First try to get single cart
        Optional<Cart> singleCart = cartRepository.findByUserEmail(userEmail);

        if (singleCart.isPresent()) {
            return singleCart.get();
        }

        // Check if multiple carts exist (shouldn't happen with unique constraint)
        List<Cart> allCarts = cartRepository.findAllByUserEmail(userEmail);

        if (allCarts.isEmpty()) {
            // No cart exists, create a new one
            return findOrCreateCart(userEmail);
        } else if (allCarts.size() == 1) {
            return allCarts.get(0);
        } else {
            // Multiple carts exist - clean up and return the best one
            System.err.println("WARNING: Found " + allCarts.size() + " carts for user: " + userEmail + ". Cleaning up duplicates.");
            return cleanupDuplicateCarts(allCarts, userEmail);
        }
    }

    @Override
    @Transactional
    public Cart addToCart(String userEmail, AddToCartRequest request) {
        // 1. Validate stock availability
        StockValidationResponse validation = inventoryClient.validateStock(
                request.getProductId(), request.getQuantity());

        if (!validation.getIsAvailable()) {
            throw new RuntimeException("Insufficient stock: " + validation.getMessage());
        }

        Cart cart = findOrCreateCart(userEmail);
        String cartOrderId = "CART-" + cart.getId();

        // 2. Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(
                cart.getId(), request.getProductId());

        if (existingItem.isPresent()) {
            // Update existing item AND adjust reservation
            CartItem item = existingItem.get();
            int oldQuantity = item.getQuantity();
            int newQuantity = oldQuantity + request.getQuantity();

            // Adjust inventory reservation
            inventoryClient.adjustReservationQuantity(cartOrderId,
                    request.getProductId(), newQuantity, userEmail);

            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            // Create new cart item AND reserve stock
            ProductResponse product = getProductInfo(request.getProductId());

            // Reserve stock for new item
            StockReservationRequest reservationRequest = new StockReservationRequest();
            reservationRequest.setProductId(request.getProductId());
            reservationRequest.setQuantity(request.getQuantity());
            reservationRequest.setOrderId(cartOrderId);
            reservationRequest.setUserEmail(userEmail);
            reservationRequest.setExpirationMinutes(60); // Longer for cart
            reservationRequest.setNotes("Cart item reservation");

            inventoryClient.reserveStock(reservationRequest);

            // Create cart item
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(product.getId());
            newItem.setProductName(product.getName());
            newItem.setQuantity(request.getQuantity());
            newItem.setUnitPrice(product.getPrice());
            newItem.calculateTotalPrice();

            cartItemRepository.save(newItem);
        }

        // Refresh and return cart
        cart = cartRepository.findByUserEmailWithItems(userEmail)
                .orElseThrow(() -> new RuntimeException("Cart not found after update"));
        cart.calculateTotalAmount();
        return cartRepository.save(cart);
    }
//
//    @Override
//    @Transactional
//    public Cart updateCartItem(String userEmail, Long cartItemId, UpdateCartItemRequest request) {
//        if (request.getQuantity() == null || request.getQuantity() <= 0) {
//            throw new RuntimeException("Quantity must be greater than 0");
//        }
//
//        Cart cart = getCartByUserEmail(userEmail);
//
//        CartItem cartItem = cartItemRepository.findById(cartItemId)
//                .orElseThrow(() -> new RuntimeException("Cart item not found"));
//
//        // Verify item belongs to user's cart
//        if (!cartItem.getCart().getUserEmail().equals(userEmail)) {
//            throw new RuntimeException("Cart item does not belong to this user");
//        }
//
//        // Check stock availability
//        ProductResponse product = getProductInfo(cartItem.getProductId());
//
//        if (product.getStockQuantity() < request.getQuantity()) {
//            throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
//        }
//
//        cartItem.setQuantity(request.getQuantity());
//        cartItem.setUnitPrice(product.getPrice()); // Update price in case it changed
//        cartItem.calculateTotalPrice();
//        cartItemRepository.save(cartItem);
//
//        // Refresh and recalculate
//        cart = cartRepository.findByUserEmailWithItems(userEmail)
//                .orElseThrow(() -> new RuntimeException("Cart not found after update"));
//        cart.calculateTotalAmount();
//        return cartRepository.save(cart);
//    }
@Override
@Transactional
public Cart updateCartItem(String userEmail, Long cartItemId, UpdateCartItemRequest request) {
    System.out.println(">>> Updating cart item ID: " + cartItemId + " for user: " + userEmail);

    // Validate input
    if (request.getQuantity() == null || request.getQuantity() <= 0) {
        throw new RuntimeException("Quantity must be greater than 0");
    }

    // Find the cart item first
    CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new RuntimeException("Cart item not found"));

    // Get the cart through the item relationship (no creation risk!)
    Cart cart = cartItem.getCart();
    System.out.println(">>> Found cart ID: " + cart.getId() + " for item: " + cartItemId);

    // Verify item belongs to user's cart
    if (!cart.getUserEmail().equals(userEmail)) {
        throw new RuntimeException("Cart item does not belong to this user");
    }

    // Check stock availability
    ProductResponse product = getProductInfo(cartItem.getProductId());
    if (product.getStockQuantity() < request.getQuantity()) {
        throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
    }

    // Update the cart item
    cartItem.setQuantity(request.getQuantity());
    cartItem.setUnitPrice(product.getPrice());
    cartItem.calculateTotalPrice();
    cartItemRepository.save(cartItem);
    System.out.println(">>> Updated cart item, new total price: " + cartItem.getTotalPrice());

    // Refresh cart from database and recalculate total
    cart = cartRepository.findById(cart.getId())
            .orElseThrow(() -> new RuntimeException("Cart not found after update"));

    cart.calculateTotalAmount();
    Cart savedCart = cartRepository.save(cart);
    System.out.println(">>> Final cart total: " + savedCart.getTotalAmount());

    return savedCart;
}

    @Override
    @Transactional
    public Cart removeFromCart(String userEmail, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        Cart cart = cartItem.getCart();
        String cartOrderId = "CART-" + cart.getId();

        // Release stock reservation
        inventoryClient.releaseReservation(cartOrderId,
                cartItem.getProductId(), userEmail);

        // Remove cart item
        cartItemRepository.delete(cartItem);

        // Refresh cart
        cart = cartRepository.findByUserEmailWithItems(userEmail)
                .orElseThrow(() -> new RuntimeException("Cart not found after update"));
        cart.calculateTotalAmount();
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void clearCart(String userEmail) {
        Optional<Cart> cartOpt = cartRepository.findByUserEmail(userEmail);
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            String cartOrderId = "CART-" + cart.getId();

            // Release all reservations
            for (CartItem item : cart.getItems()) {
                inventoryClient.releaseReservation(cartOrderId,
                        item.getProductId(), userEmail);
            }

            // Clear cart items
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
            cart.calculateTotalAmount();
            cartRepository.save(cart);
        }
    }

    /**
     * Find existing cart or create new one (atomic operation)
     */
    @Transactional
    private Cart findOrCreateCart(String userEmail) {
        // Try to find existing cart first
        Optional<Cart> existingCart = cartRepository.findByUserEmail(userEmail);
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        // Double-check to prevent race conditions
        if (cartRepository.existsByUserEmail(userEmail)) {
            return cartRepository.findByUserEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Cart lookup failed"));
        }

        // Create new cart
        Cart newCart = new Cart();
        newCart.setUserEmail(userEmail);

        try {
            return cartRepository.save(newCart);
        } catch (Exception e) {
            // Handle unique constraint violation - someone else created cart
            System.err.println("Cart creation failed (likely race condition): " + e.getMessage());
            return cartRepository.findByUserEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Failed to create or find cart"));
        }
    }

    /**
     * Clean up duplicate carts - keep the one with most items or latest
     */
    @Transactional
    private Cart cleanupDuplicateCarts(List<Cart> carts, String userEmail) {
        // Sort by: 1) number of items (desc), 2) created date (desc)
        Cart bestCart = carts.stream()
                .max(Comparator
                        .comparing((Cart c) -> c.getItems().size())
                        .thenComparing(Cart::getCreatedAt))
                .orElse(carts.get(0));

        // Delete other carts
        carts.stream()
                .filter(cart -> !cart.getId().equals(bestCart.getId()))
                .forEach(cart -> {
                    System.err.println("Deleting duplicate cart ID: " + cart.getId() + " for user: " + userEmail);
                    cartItemRepository.deleteAll(cart.getItems());
                    cartRepository.delete(cart);
                });

        return bestCart;
    }

    /**
     * Get product information with error handling
     */
    private ProductResponse getProductInfo(Long productId) {
        try {
            return productServiceClient.getProductById(productId);
        } catch (Exception e) {
            throw new RuntimeException("Product with ID " + productId + " not found or service unavailable: " + e.getMessage());
        }
    }
}