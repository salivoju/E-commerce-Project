package com.ecommerce.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class ApiGatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()  // Allow preflight
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/api/v1/users/register").permitAll()
                        .anyExchange().authenticated()
                )
                .cors(Customizer.withDefaults())  // Enables CORS
                .build();
    }
}
