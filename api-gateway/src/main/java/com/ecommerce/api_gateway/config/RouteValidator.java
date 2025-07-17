package com.ecommerce.api_gateway.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> openApiEndpoints = List.of(
            "/api/v1/auth/login",
            "/api/v1/users/register",  // Public user registration
            "/api/v1/users/health",
            "/eureka"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> !request.getMethod().name().equals("OPTIONS") && openApiEndpoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));

}