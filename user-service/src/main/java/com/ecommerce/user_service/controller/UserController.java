package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.model.User;
import com.ecommerce.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * PUBLIC: User registration (no authentication required)
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            System.out.println(">>> Public registration for: " + user.getEmail());

            // Check if user already exists
            Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
            if (existingUser.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "User with email " + user.getEmail() + " already exists"));
            }

            // Hash password and save
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User savedUser = userRepository.save(user);
            System.out.println(">>> User registered successfully: " + savedUser.getEmail());
            return new ResponseEntity<>(savedUser, HttpStatus.CREATED);

        } catch (Exception e) {
            System.err.println(">>> Registration error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    /**
     * ADMIN ONLY: Create user (requires ROLE_ADMIN from API Gateway)
     */
    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestBody User user,
            @RequestHeader(value = "X-Authenticated-User-Username", required = false) String userEmail,
            @RequestHeader(value = "X-Authenticated-User-Roles", required = false) String userRoles) {

        System.out.println(">>> Create user request from: " + userEmail);
        System.out.println(">>> User roles: " + userRoles);

        // If no headers, this came directly to service (not through gateway)
        if (userEmail == null || userRoles == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Direct access not allowed. Use API Gateway."));
        }

        // Check if user has admin role
        if (!userRoles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required to create users"));
        }

        // Check if user already exists
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "User with email " + user.getEmail() + " already exists"));
        }

        // Hash password and save
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    /**
     * ADMIN ONLY: Get all users
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "X-Authenticated-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-Authenticated-User-Username", required = false) String userEmail) {

        System.out.println(">>> Get all users request from: " + userEmail);

        // Allow if no headers (for backward compatibility during development)
        if (userRoles == null) {
            List<User> users = userRepository.findAll();
            return ResponseEntity.ok(users);
        }

        // Check if user has admin role
        if (!userRoles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required to view all users"));
        }

        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * AUTHENTICATED: Get user by ID (own profile or admin)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable long id,
            @RequestHeader(value = "X-Authenticated-User-Username", required = false) String currentUserEmail,
            @RequestHeader(value = "X-Authenticated-User-Roles", required = false) String userRoles) {

        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = optionalUser.get();

        // If no headers, allow (for development)
        if (currentUserEmail == null) {
            return ResponseEntity.ok(user);
        }

        // Admin can view any user, regular users can only view their own profile
        if (!userRoles.contains("ROLE_ADMIN") && !user.getEmail().equals(currentUserEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only view your own profile"));
        }

        return ResponseEntity.ok(user);
    }

    /**
     * AUTHENTICATED: Update user (own profile or admin)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserById(
            @PathVariable long id,
            @RequestBody User user,
            @RequestHeader(value = "X-Authenticated-User-Username", required = false) String currentUserEmail,
            @RequestHeader(value = "X-Authenticated-User-Roles", required = false) String userRoles) {

        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User existingUser = optionalUser.get();

        // If no headers, allow (for development)
        if (currentUserEmail == null) {
            existingUser.setEmail(user.getEmail());
            existingUser.setName(user.getName());
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            User updatedUser = userRepository.save(existingUser);
            return ResponseEntity.ok(updatedUser);
        }

        // Admin can update any user, regular users can only update their own profile
        if (!userRoles.contains("ROLE_ADMIN") && !existingUser.getEmail().equals(currentUserEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only update your own profile"));
        }

        // Update fields
        existingUser.setEmail(user.getEmail());
        existingUser.setName(user.getName());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Only admin can change roles
        if (user.getRole() != null && userRoles.contains("ROLE_ADMIN")) {
            existingUser.setRole(user.getRole());
        }

        User updatedUser = userRepository.save(existingUser);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * ADMIN ONLY: Delete user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserById(
            @PathVariable long id,
            @RequestHeader(value = "X-Authenticated-User-Roles", required = false) String userRoles) {

        // If no headers, deny (deletion is dangerous)
        if (userRoles == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required for user deletion"));
        }

        // Check if user has admin role
        if (!userRoles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required to delete users"));
        }

        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DEVELOPMENT: Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "user-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}