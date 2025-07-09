package com.ecommerce.cart_service.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.ecommerce.order_service.dto.ProductResponse;

@FeignClient(name = "product-catalog-service")
public interface ProductServiceClient {
    @GetMapping("/api/v1/products/{id}")
    ProductResponse getProductById(@PathVariable("id") Long productId);
}