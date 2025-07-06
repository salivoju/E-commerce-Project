package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client to communicate with Product Catalog Service
 * The service name should match the one registered in Eureka
 */
@FeignClient(name = "product-catalog-service")
public interface ProductServiceClient {

    /**
     * Get product details by ID from Product Service
     * @param productId The ID of the product
     * @return ProductResponse containing product details
     */
    @GetMapping("/api/v1/products/{id}")
    ProductResponse getProductById(@PathVariable("id") Long productId);
}