package com.ecommerce.user_service.service;

import com.ecommerce.user_service.repository.UserRepository;
import com.ecommerce.user_service.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * JWT Service
 *
 * Handles JWT token generation, validation, and claim extraction.
 * Provides secure token management for user authentication.
 *
 * Features:
 * - Token generation with user claims (role, email, name)
 * - Token validation and expiration checking
 * - Secure key management
 * - Comprehensive error handling
 *
 * @author E-commerce Team
 * @version 1.0
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Autowired
    private UserRepository userRepository;

    /**
     * Extract username from JWT token
     *
     * @param token JWT token
     * @return Username (email)
     */
    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            logger.error("Error extracting username from token: {}", e.getMessage());
            throw new RuntimeException("Invalid token format", e);
        }
    }

    /**
     * Extract expiration date from JWT token
     *
     * @param token JWT token
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim from JWT token
     *
     * @param token JWT token
     * @param claimsResolver Function to extract claim
     * @param <T> Type of claim
     * @return Extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: {}", e.getMessage());
            throw new RuntimeException("Token expired", e);
        } catch (MalformedJwtException e) {
            logger.error("JWT token is malformed: {}", e.getMessage());
            throw new RuntimeException("Malformed token", e);
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
            throw new RuntimeException("Unsupported token", e);
        } catch (SignatureException e) {
            logger.error("JWT signature validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid token signature", e);
        } catch (Exception e) {
            logger.error("Error parsing JWT token: {}", e.getMessage());
            throw new RuntimeException("Token parsing error", e);
        }
    }

    /**
     * Generate JWT token for username with embedded user information
     *
     * @param username Username (email)
     * @return JWT token
     */
    public String generateToken(String username) {
        try {
            // Get user from database to include role and other info in token
            Optional<User> userOpt = userRepository.findByEmail(username);
            Map<String, Object> claims = new HashMap<>();

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                claims.put("role", user.getRole().name());
                claims.put("email", user.getEmail());
                claims.put("name", user.getName());
                logger.debug("Generating token for user: {} with role: {}", username, user.getRole());
            } else {
                logger.warn("User not found when generating token: {}", username);
                throw new RuntimeException("User not found: " + username);
            }

            return createToken(claims, username);
        } catch (Exception e) {
            logger.error("Error generating token for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Generate JWT token with extra claims for UserDetails
     *
     * @param extraClaims Additional claims
     * @param userDetails Spring Security UserDetails
     * @return JWT token
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        try {
            // Merge extra claims with user-specific claims
            Map<String, Object> allClaims = new HashMap<>(extraClaims);

            // Get user from database to ensure we have the role
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                allClaims.put("role", user.getRole().name());
                allClaims.put("email", user.getEmail());
                allClaims.put("name", user.getName());
            }

            return createToken(allClaims, userDetails.getUsername());
        } catch (Exception e) {
            logger.error("Error generating token for UserDetails {}: {}", userDetails.getUsername(), e.getMessage());
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Create JWT token with claims and subject
     *
     * @param claims Token claims
     * @param subject Token subject (username)
     * @return JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        try {
            Date now = new Date(System.currentTimeMillis());
            Date expiryDate = new Date(now.getTime() + expiration);

            String token = Jwts.builder()
                    .claims(claims)
                    .subject(subject)
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(getSigningKey())
                    .compact();

            logger.debug("Token created successfully for subject: {}, expires at: {}", subject, expiryDate);
            return token;
        } catch (Exception e) {
            logger.error("Error creating JWT token: {}", e.getMessage());
            throw new RuntimeException("Token creation failed", e);
        }
    }

    /**
     * Extract all claims from JWT token
     *
     * @param token JWT token
     * @return All claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get signing key for JWT token
     *
     * @return Secret key
     */
    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            logger.error("Error creating signing key: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT secret configuration", e);
        }
    }

    /**
     * Check if JWT token is expired
     *
     * @param token JWT token
     * @return true if expired, false otherwise
     */
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            boolean expired = expiration.before(new Date());
            if (expired) {
                logger.debug("Token is expired. Expiration: {}, Current: {}", expiration, new Date());
            }
            return expired;
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true; // Consider invalid tokens as expired
        }
    }

    /**
     * Validate JWT token against UserDetails
     *
     * @param token JWT token
     * @param userDetails Spring Security UserDetails
     * @return true if valid, false otherwise
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
            logger.debug("Token validation result for user {}: {}", username, isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract user role from JWT token
     *
     * @param token JWT token
     * @return User role
     */
    public String extractRole(String token) {
        try {
            return extractClaim(token, claims -> claims.get("role", String.class));
        } catch (Exception e) {
            logger.error("Error extracting role from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract user email from JWT token
     *
     * @param token JWT token
     * @return User email
     */
    public String extractEmail(String token) {
        try {
            return extractClaim(token, claims -> claims.get("email", String.class));
        } catch (Exception e) {
            logger.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract user name from JWT token
     *
     * @param token JWT token
     * @return User name
     */
    public String extractName(String token) {
        try {
            return extractClaim(token, claims -> claims.get("name", String.class));
        } catch (Exception e) {
            logger.error("Error extracting name from token: {}", e.getMessage());
            return null;
        }
    }
}