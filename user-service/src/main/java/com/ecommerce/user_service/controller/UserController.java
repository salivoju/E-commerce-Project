package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.model.User;
import com.ecommerce.user_service.model.Role;
import com.ecommerce.user_service.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Health check endpoint for load balancer
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "user-service",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    /**
     * PUBLIC: User registration (no authentication required)
     * Anyone can register as a regular user
     *
     * @param user User registration data
     * @return ResponseEntity with created user or error
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        logger.info("User registration attempt for email: {}", user.getEmail());

        try {
            // Validate email format and required fields
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email is required"));
            }

            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password is required"));
            }

            if (user.getName() == null || user.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Name is required"));
            }

            // Check if user already exists
            Optional<User> existingUser = userRepository.findByEmail(user.getEmail().trim().toLowerCase());
            if (existingUser.isPresent()) {
                logger.warn("Registration attempt with existing email: {}", user.getEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "User with email " + user.getEmail() + " already exists"));
            }

            // Create a new User object to ensure clean state
            User newUser = new User();

            // Set basic fields
            newUser.setEmail(user.getEmail().trim().toLowerCase());
            newUser.setName(user.getName().trim());
            newUser.setPassword(passwordEncoder.encode(user.getPassword()));

            // Set optional profile fields if provided
            if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) {
                newUser.setFirstName(user.getFirstName().trim());
            }
            if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
                newUser.setLastName(user.getLastName().trim());
            }
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
                newUser.setPhoneNumber(user.getPhoneNumber().trim());
            }

            // Set role - always ROLE_USER for public registration
            newUser.setRole(Role.ROLE_USER);

            // EXPLICITLY set all required boolean fields with defaults
            newUser.setEnabled(true);
            newUser.setAccountNonExpired(true);
            newUser.setAccountNonLocked(true);
            newUser.setCredentialsNonExpired(true);

            // Set timestamps manually since @PrePersist might not be working
            LocalDateTime now = LocalDateTime.now();
            newUser.setCreatedAt(now);
            newUser.setUpdatedAt(now);

            // Save the user
            User savedUser = userRepository.save(newUser);

            // Remove password from response
            savedUser.setPassword(null);

            logger.info("User registered successfully: {}", savedUser.getEmail());
            return new ResponseEntity<>(savedUser, HttpStatus.CREATED);

        } catch (Exception e) {
            logger.error("Registration error for email {}: {}", user.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    /**
     * ADMIN ONLY: Create user (requires ROLE_ADMIN from API Gateway)
     * Admins can create users with any role
     *
     * @param user User data
     * @param userEmail Current user email from JWT (via API Gateway)
     * @param userRoles Current user roles from JWT (via API Gateway)
     * @return ResponseEntity with created user or error
     */
    @PostMapping
    public ResponseEntity<?> createUser(
            @Valid @RequestBody User user,
            @RequestHeader(value = "X-Authenticated-User-Username", required = false) String userEmail,
            @RequestHeader(value = "X-Authenticated-User-Roles", required = false) String userRoles) {

        logger.info("Create user request from: {} with roles: {}", userEmail, userRoles);

        try {
            // If no headers, this came directly to service (not through gateway)
            if (userEmail == null || userRoles == null) {
                logger.warn("Direct access attempt to create user endpoint");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Direct access not allowed. Use API Gateway."));
            }

            // Check if user has admin role
            if (!userRoles.contains("ROLE_ADMIN")) {
                logger.warn("Non-admin user {} attempted to create user", userEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required to create users"));
            }

            // Validate required fields
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email is required"));
            }

            // Check if user already exists
            Optional<User> existingUser = userRepository.findByEmail(user.getEmail().trim().toLowerCase());
            if (existingUser.isPresent()) {
                logger.warn("Admin {} attempted to create existing user: {}", userEmail, user.getEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "User with email " + user.getEmail() + " already exists"));
            }

            // Sanitize and set user data
            user.setEmail(user.getEmail().trim().toLowerCase());
            user.setName(user.getName() != null ? user.getName().trim() : "");

            // Hash password and save
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User savedUser = userRepository.save(user);

            // Remove password from response
            savedUser.setPassword(null);

            logger.info("User created successfully by admin {}: {}", userEmail, savedUser.getEmail());
            return new ResponseEntity<>(savedUser, HttpStatus.CREATED);

        } catch (Exception e) {
            logger.error("Error creating user by admin {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "User creation failed: " + e.getMessage()));
        }
    }

    /**
     * ADMIN ONLY: Get all users
     *
     * @param userRoles Current user roles from JWT (via API Gateway)
     * @param userEmail Current user email from JWT (via API Gateway)
     * @return ResponseEntity with list of users or error
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "X-Authenticated-User-Roles", required = false) String userRoles,
            @RequestHeader(value = "X-Authenticated-User-Username", required = false) String userEmail) {

        logger.info("Get all users request from: {} with roles: {}", userEmail, userRoles);

        try {
            // Allow if no headers (for backward compatibility during development)
            if (userRoles == null) {
                logger.debug("No role headers present, allowing access (development mode)");
                List<User> users = userRepository.findAll();
                // Remove passwords from response
                users.forEach(user -> user.setPassword(null));
                return ResponseEntity.ok(users);
            }

            // Check if user has admin role
            if (!userRoles.contains("ROLE_ADMIN")) {
                logger.warn("Non-admin user {} attempted to get all users", userEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required to view all users"));
            }

            List<User> users = userRepository.findAll();
            // Remove passwords from response
            users.forEach(user -> user.setPassword(null));

            logger.info("Admin {} retrieved {} users", userEmail, users.size());
            return ResponseEntity.ok(users);

        } catch (Exception e) {
            logger.error("Error getting all users for admin {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve users: " + e.getMessage()));
        }
    }

    /**
     * AUTHENTICATED: Get user by ID (own profile or admin)
     *
     * @param id User ID
     * @param currentUserEmail Current user email from JWT (via API Gateway)
     * @param userRoles Current user roles from JWT (via API Gateway)
     * @return ResponseEntity with user data or error
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable long id,
            @RequestHeader(value = "X-Authenticated-User-Username", required = false) String currentUserEmail,
            @RequestHeader(value = "X-Authenticated-User-Roles", required = false) String userRoles) {

        logger.info("Get user by ID {} request from: {}", id, currentUserEmail);

        try {
            Optional<User> optionalUser = userRepository.findById(id);
            if (optionalUser.isEmpty()) {
                logger.warn("User with ID {} not found", id);
                return ResponseEntity.notFound().build();
            }

            User user = optionalUser.get();

            // If no headers, allow (for development)
            if (currentUserEmail == null) {
                logger.debug("No auth headers present, allowing access (development mode)");
                user.setPassword(null);
                return ResponseEntity.ok(user);
            }

            // Admin can view any user, regular users can only view their own profile
            if (!userRoles.contains("ROLE_ADMIN") && !user.getEmail().equals(currentUserEmail)) {
                logger.warn("User {} attempted to access profile of user {}", currentUserEmail, user.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only view your own profile"));
            }

            // Remove password from response
            user.setPassword(null);

            logger.info("User profile {} accessed by {}", user.getEmail(), currentUserEmail);
            return ResponseEntity.ok(user);

        } catch (Exception e) {
            logger.error("Error getting user by ID {} for user {}: {}", id, currentUserEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve user: " + e.getMessage()));
        }
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

        logger.info("Update user {} request from: {}", id, currentUserEmail);

        try {
            // Check if user exists
            Optional<User> optionalUser = userRepository.findById(id);
            if (optionalUser.isEmpty()) {
                logger.warn("Attempt to update non-existent user with ID {}", id);
                return ResponseEntity.notFound().build();
            }

            User existingUser = optionalUser.get();

            // Development mode - no auth headers
            if (currentUserEmail == null) {
                logger.debug("No auth headers present, allowing update (development mode)");
                updateUserFields(existingUser, user, userRoles); // Allow all updates in dev mode
                User updatedUser = userRepository.save(existingUser);
                updatedUser.setPassword(null); // Remove password from response
                return ResponseEntity.ok(updatedUser);
            }

            // Authorization check
            boolean isAdmin = userRoles != null && userRoles.contains("ROLE_ADMIN");
            logger.info("🔍 DEBUG - userRoles: '{}', isAdmin: {}", userRoles, isAdmin);
            boolean isOwner = existingUser.getEmail().equals(currentUserEmail);

            if (!isAdmin && !isOwner) {
                logger.warn("User {} attempted to update profile of user {}", currentUserEmail, existingUser.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only update your own profile"));
            }

            // Update user fields with appropriate permissions
            updateUserFields(existingUser, user, userRoles);

            // Save updated user
            User updatedUser = userRepository.save(existingUser);
            updatedUser.setPassword(null); // Remove password from response

            logger.info("User {} updated by {}", existingUser.getEmail(), currentUserEmail);
            return ResponseEntity.ok(updatedUser);

        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation while updating user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already exists or data constraint violated"));
        } catch (Exception e) {
            logger.error("Error updating user {} by {}: {}", id, currentUserEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "User update failed: " + e.getMessage()));
        }
    }

    /**
     * Helper method to update user fields with proper permission controls
     *
     * @param existingUser Existing user from database
     * @param newUserData New user data from request
     * @param userRoles Current user roles from JWT (for admin check)
     */
    private void updateUserFields(User existingUser, User newUserData, String userRoles) {
        // Basic fields (all users can update)
        if (newUserData.getEmail() != null && !newUserData.getEmail().trim().isEmpty()) {
            existingUser.setEmail(newUserData.getEmail().trim().toLowerCase());
        }

        if (newUserData.getName() != null && !newUserData.getName().trim().isEmpty()) {
            existingUser.setName(newUserData.getName().trim());
        }

        if (newUserData.getPassword() != null && !newUserData.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(newUserData.getPassword()));
        }

        // Extended profile fields (all users can update)
        if (newUserData.getFirstName() != null && !newUserData.getFirstName().trim().isEmpty()) {
            existingUser.setFirstName(newUserData.getFirstName().trim());
        }

        if (newUserData.getLastName() != null && !newUserData.getLastName().trim().isEmpty()) {
            existingUser.setLastName(newUserData.getLastName().trim());
        }

        if (newUserData.getPhoneNumber() != null && !newUserData.getPhoneNumber().trim().isEmpty()) {
            existingUser.setPhoneNumber(newUserData.getPhoneNumber().trim());
        }

        // Admin-only fields - SECURE IMPLEMENTATION
        boolean isAdmin = userRoles != null && userRoles.contains("ROLE_ADMIN");

        if (isAdmin) {
            // Role update (admin only)
            if (newUserData.getRole() != null) {
                existingUser.setRole(newUserData.getRole());
                logger.debug("Role updated to {} for user: {}", newUserData.getRole(), existingUser.getEmail());
            }

            // Account status fields (admin only)
            if (newUserData.getEnabled() != null) {
                existingUser.setEnabled(newUserData.getEnabled());
            }

            if (newUserData.getAccountNonExpired() != null) {
                existingUser.setAccountNonExpired(newUserData.getAccountNonExpired());
            }

            if (newUserData.getAccountNonLocked() != null) {
                existingUser.setAccountNonLocked(newUserData.getAccountNonLocked());
            }

            if (newUserData.getCredentialsNonExpired() != null) {
                existingUser.setCredentialsNonExpired(newUserData.getCredentialsNonExpired());
            }
        } else {
            // Log unauthorized attempts
            if (newUserData.getRole() != null) {
                logger.warn("Non-admin user attempted to update role for user: {}", existingUser.getEmail());
                throw new RuntimeException("Only administrators can update user roles");
            }
            if (newUserData.getEnabled() != null) {
                logger.warn("Non-admin user attempted to update account status for user: {}", existingUser.getEmail());
            }
        }
    }
//    /**
//     * Helper method to update user fields based on permissions
//     * @param existingUser The user entity to update
//     * @param updatedUser The user data containing updates
//     * @param isAdmin Whether the requesting user has admin privileges
//     */
//    private void updateUserFields(User existingUser, User updatedUser, boolean isAdmin) {
//        // Basic profile fields (all users can update these)
//        if (updatedUser.getName() != null && !updatedUser.getName().trim().isEmpty()) {
//            existingUser.setName(updatedUser.getName().trim());
//        }
//
//        if (updatedUser.getEmail() != null && !updatedUser.getEmail().trim().isEmpty()) {
//            existingUser.setEmail(updatedUser.getEmail().trim().toLowerCase());
//        }
//
//        if (updatedUser.getFirstName() != null) {
//            existingUser.setFirstName(updatedUser.getFirstName().trim().isEmpty() ? null : updatedUser.getFirstName().trim());
//        }
//
//        if (updatedUser.getLastName() != null) {
//            existingUser.setLastName(updatedUser.getLastName().trim().isEmpty() ? null : updatedUser.getLastName().trim());
//        }
//
//        if (updatedUser.getPhoneNumber() != null) {
//            existingUser.setPhoneNumber(updatedUser.getPhoneNumber().trim().isEmpty() ? null : updatedUser.getPhoneNumber().trim());
//        }
//
//        // Password update (all users can update their own password)
//        if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
//            String encodedPassword = passwordEncoder.encode(updatedUser.getPassword());
//            existingUser.setPassword(encodedPassword);
//            logger.debug("Password updated for user: {}", existingUser.getEmail());
//        }
//
//        // Admin-only fields
//        if (isAdmin) {
//            // Role update (admin only)
//            if (updatedUser.getRole() != null) {
//                existingUser.setRole(updatedUser.getRole());
//                logger.debug("Role updated to {} for user: {}", updatedUser.getRole(), existingUser.getEmail());
//            }
//
//            // Account status fields (admin only)
//            if (updatedUser.getEnabled() != null) {
//                existingUser.setEnabled(updatedUser.getEnabled());
//                logger.debug("Account enabled status updated to {} for user: {}", updatedUser.getEnabled(), existingUser.getEmail());
//            }
//
//            if (updatedUser.getAccountNonExpired() != null) {
//                existingUser.setAccountNonExpired(updatedUser.getAccountNonExpired());
//            }
//
//            if (updatedUser.getAccountNonLocked() != null) {
//                existingUser.setAccountNonLocked(updatedUser.getAccountNonLocked());
//            }
//
//            if (updatedUser.getCredentialsNonExpired() != null) {
//                existingUser.setCredentialsNonExpired(updatedUser.getCredentialsNonExpired());
//            }
//        } else {
//            // Log attempt to update admin-only fields by non-admin users
//            if (updatedUser.getRole() != null) {
//                logger.warn("Non-admin user attempted to update role for user: {}", existingUser.getEmail());
//            }
//            if (updatedUser.getEnabled() != null) {
//                logger.warn("Non-admin user attempted to update account status for user: {}", existingUser.getEmail());
//            }
//        }
//    }
}