package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.dto.AuthRequest;
import com.ecommerce.user_service.dto.JwtResponse;
import com.ecommerce.user_service.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public JwtResponse authenticateAndGetToken(@RequestBody AuthRequest authRequest) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        if (authentication.isAuthenticated()) {
            // Generate token with user's role embedded
            String token = jwtService.generateToken(authentication.getName());
            return JwtResponse.builder().accessToken(token).build();
        } else {
            throw new UsernameNotFoundException("Invalid user request!");
        }
    }
}