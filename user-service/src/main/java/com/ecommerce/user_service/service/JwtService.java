package com.ecommerce.user_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // This is a secret key for signing the JWT.
    // IMPORTANT: In a real production application, this key MUST be stored securely
    // and should not be hardcoded. It should be much longer and more complex.
    public static final String SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts
                .builder()
                .claims(claims)                    // Changed from setClaims()
                .subject(subject)                  // Changed from setSubject()
                .issuedAt(new Date(System.currentTimeMillis()))     // Changed from setIssuedAt()
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24)) // Changed from setExpiration(), Token valid for 24 hours
                .signWith(getSigningKey())         // Removed SignatureAlgorithm parameter
                .compact();
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts
                .builder()
                .claims(extraClaims)               // Changed from setClaims()
                .subject(userDetails.getUsername()) // Changed from setSubject()
                .issuedAt(new Date(System.currentTimeMillis()))     // Changed from setIssuedAt()
                // Token is valid for 24 hours
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24))  // Changed from setExpiration()
                .signWith(getSigningKey())         // Removed SignatureAlgorithm parameter
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()                          // Changed from parserBuilder()
                .verifyWith(getSigningKey())       // Changed from setSigningKey()
                .build()
                .parseSignedClaims(token)          // Changed from parseClaimsJws()
                .getPayload();                     // Changed from getBody()
    }

    private SecretKey getSigningKey() {            // Changed return type from Key to SecretKey
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}