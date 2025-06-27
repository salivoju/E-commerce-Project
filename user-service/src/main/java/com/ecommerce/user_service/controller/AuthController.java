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
        // We use the AuthenticationManager to validate the username and password.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        // If authentication is successful, we generate a token.
        if (authentication.isAuthenticated()) {
            String token = jwtService.generateToken(authentication.getName());
            return JwtResponse.builder().accessToken(token).build();
        } else {
            // If authentication fails, we throw an exception.
            throw new UsernameNotFoundException("Invalid user request!");
        }
    }
}