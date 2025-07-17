package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.dto.AuthRequest;
import com.ecommerce.user_service.dto.JwtResponse;
import com.ecommerce.user_service.service.JwtService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Controller
 *
 * Handles user authentication and JWT token generation.
 * Provides secure login functionality with comprehensive error handling.
 *
 * Features:
 * - User authentication with email/password
 * - JWT token generation with user claims
 * - Comprehensive error handling
 * - Security logging
 * - Input validation
 *
 * API Endpoints:
 * - POST /api/v1/auth/login - User authentication
 *
 * @author E-commerce Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    /**
     * Authenticate user and generate JWT token
     *
     * @param authRequest Authentication request containing username and password
     * @return JwtResponse with access token or error response
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateAndGetToken(@Valid @RequestBody AuthRequest authRequest) {
        String username = authRequest.getUsername();
        logger.info("Authentication attempt for user: {}", username);

        try {
            // Validate input
            if (username == null || username.trim().isEmpty()) {
                logger.warn("Authentication attempt with empty username");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username is required"));
            }

            if (authRequest.getPassword() == null || authRequest.getPassword().trim().isEmpty()) {
                logger.warn("Authentication attempt with empty password for user: {}", username);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password is required"));
            }

            // Sanitize username (email)
            username = username.trim().toLowerCase();

            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, authRequest.getPassword())
            );

            if (authentication.isAuthenticated()) {
                // Generate token with user's role and information embedded
                String token = jwtService.generateToken(authentication.getName());

                logger.info("Authentication successful for user: {}", username);
                return ResponseEntity.ok(JwtResponse.builder().accessToken(token).build());

            } else {
                logger.warn("Authentication failed for user: {} - Invalid credentials", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }

        } catch (BadCredentialsException e) {
            logger.warn("Authentication failed for user: {} - Bad credentials", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));

        } catch (DisabledException e) {
            logger.warn("Authentication failed for user: {} - Account disabled", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Account is disabled"));

        } catch (LockedException e) {
            logger.warn("Authentication failed for user: {} - Account locked", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Account is locked"));

        } catch (UsernameNotFoundException e) {
            logger.warn("Authentication failed for user: {} - User not found", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));

        } catch (AuthenticationException e) {
            logger.error("Authentication error for user: {} - {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed"));

        } catch (Exception e) {
            logger.error("Unexpected error during authentication for user: {} - {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication service temporarily unavailable"));
        }
    }

    /**
     * Validate JWT token endpoint (for other services)
     *
     * @param authorizationHeader Authorization header with Bearer token
     * @return Token validation result
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid authorization header"));
            }

            String token = authorizationHeader.substring(7);
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);
            String email = jwtService.extractEmail(token);
            String name = jwtService.extractName(token);

            if (username != null && !jwtService.isTokenExpired(token)) {
                logger.debug("Token validation successful for user: {}", username);
                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "username", username,
                        "role", role != null ? role : "",
                        "email", email != null ? email : "",
                        "name", name != null ? name : ""
                ));
            } else {
                logger.warn("Token validation failed - expired or invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "error", "Token expired or invalid"));
            }

        } catch (Exception e) {
            logger.error("Error validating token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Token validation failed"));
        }
    }

    /**
     * Logout endpoint (mainly for logging purposes)
     * Since JWT is stateless, actual logout is handled client-side
     *
     * @param authorizationHeader Authorization header with Bearer token
     * @return Logout confirmation
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                String username = jwtService.extractUsername(token);
                logger.info("User logged out: {}", username);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Logged out successfully",
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            logger.debug("Logout attempt with invalid token: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "message", "Logged out successfully",
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }
}