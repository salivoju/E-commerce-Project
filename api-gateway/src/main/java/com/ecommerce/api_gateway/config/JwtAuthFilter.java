package com.ecommerce.api_gateway.config;

import com.ecommerce.api_gateway.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtService jwtService;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            // Check if the request is to a secured endpoint
            if (validator.isSecured.test(exchange.getRequest())) {
                // Check if the request has the Authorization header
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    // If header is missing, return an error response immediately
                    return this.onError(exchange, "Missing authorization header");
                }

                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                String token = null;
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                    System.out.println("Extracted token: " + token.substring(0, Math.min(token.length(), 20)) + "...");
                } else {
                    System.out.println("Invalid auth header format: " + authHeader);
                    return this.onError(exchange, "Invalid authorization header format");
                }

                try {
                    // Validate the token using the JwtService
                    jwtService.validateToken(token);

                    // Extract user information from the token
                    String username = jwtService.extractUsername(token);

                    // Add user info to request headers for downstream services
                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .header("X-Authenticated-User-Username", username)
                            .build();

                    // Continue with modified request
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());

                } catch (Exception e) {
                    // Log the actual error for debugging
                    System.err.println("JWT Validation Error: " + e.getMessage());
                    e.printStackTrace();
                    // If token is invalid, return an unauthorized error response
                    return this.onError(exchange, "Unauthorized access to application: " + e.getMessage());
                }
            }
            // If the endpoint is not secured, continue the filter chain
            return chain.filter(exchange);
        });
    }

    // This helper method creates a proper HTTP error response.
    private Mono<Void> onError(ServerWebExchange exchange, String err) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        // Add error message to response body as proper JSON
        String jsonError = "{\"error\":\"" + err + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(jsonError.getBytes(StandardCharsets.UTF_8));
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
    }
}