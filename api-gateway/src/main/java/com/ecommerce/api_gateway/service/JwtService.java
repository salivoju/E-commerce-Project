package com.ecommerce.api_gateway.service;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JwtService {

    public static final String SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject(); // or .get("email", String.class) if email is stored differently
    }

    public void validateToken(String token) {
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);

    }
    private SecretKey getSigningKey() {            // Changed return type from Key to SecretKey
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

//    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
//        final Claims claims = extractAllClaims(token);
//        return claimsResolver.apply(claims);

//    }

//    private Claims extractAllClaims(String token) {
//        return Jwts
//                .parser()                          // Changed from parserBuilder()
//                .verifyWith(getSigningKey())       // Changed from setSigningKey()
//                .build()
//                .parseSignedClaims(token)          // Changed from parseClaimsJws()
//                .getPayload();                     // Changed from getBody()
//    }


}