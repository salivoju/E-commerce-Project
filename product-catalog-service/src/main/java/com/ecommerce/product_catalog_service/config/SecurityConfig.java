package com.ecommerce.product_catalog_service.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF (Cross-Site Request Forgery)
                .csrf(csrf -> csrf.disable())

                // 2. Configure Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        // Rule 1: Allow POST requests to /api/v1/products ONLY for users with the 'ADMIN' role.
                        // Note: .hasRole("ADMIN") automatically checks for "ROLE_ADMIN".
                        .requestMatchers(HttpMethod.POST, "/api/v1/products").hasRole("ADMIN")
                        // Rule 2: All other requests (like GET) just need to be authenticated.
                        .anyRequest().authenticated()

                );

        return http.build();
    }
}