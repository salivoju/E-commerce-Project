package com.ecommerce.product_catalog_service.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        // For now, we will permit ALL requests.
                        // We will add more specific rules later.
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}