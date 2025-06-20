package com.ecommerce.product_catalog_service;

import jakarta.persistence.Id;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * This is our Controller class, which exposes our REST API endpoints.
 * @RestController is a convenience annotation that combines @Controller and @ResponseBody.
 * @RequestMapping defines a base path for all endpoints in this controller.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    // We use @Autowired to ask Spring to inject an instance of our ProductRepository.
    @Autowired
    private ProductRepository productRepository;

    /**
     * This method handles HTTP POST requests to /api/v1/products.
     * @PostMapping marks this method to handle POST requests.
     * @RequestBody tells Spring to convert the JSON from the request body into a Product object.
     * @return The saved product object and an HTTP status code of 201 (CREATED).
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product savedProduct = productRepository.save(product);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Product> getAllProducts(){
        return productRepository.findAll();
    }

    /**
     * This method handles HTTP GET requests to /api/v1/products/{id}.
     * The {id} is a path variable.
     * @param id The product ID, captured from the URL path.
     * @return A ResponseEntity containing the found product with a 200 OK status,
     * or a 404 NOT FOUND status if the product doesn't exist.
     */

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id){
        Optional<Product> productOptional = productRepository.findById(id);
        if(productOptional.isPresent()){
            return ResponseEntity.ok(productOptional.get());
        }
        else{
            return ResponseEntity.notFound().build();
        }
    }
//    return productOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());

    /**
     * This method handles HTTP PUT requests to /api/v1/products/{id} for updating a product.
     * @PutMapping marks this method to handle PUT requests for a specific resource.
     * @param id The ID of the product to update, from the URL path.
     * @param productDetails The new product data sent in the request body.
     * @return A ResponseEntity containing the updated product with a 200 OK status,
     * or a 404 NOT FOUND status if the product doesn't exist.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProductById(@PathVariable Long id,@RequestBody Product productDetails){
        Optional<Product> productOptional = productRepository.findById(id);

        if(productOptional.isPresent()){
            Product existingProduct = productOptional.get();

            existingProduct.setName(productDetails.getName());
            existingProduct.setDescription(productDetails.getDescription());
            existingProduct.setPrice(productDetails.getPrice());
            existingProduct.setStockQuantity(productDetails.getStockQuantity());

            Product updateProduct = productRepository.save(existingProduct);
            return ResponseEntity.ok(updateProduct);
        }
        else{
            return ResponseEntity.notFound().build();
        }

    }


    /**
     * This method handles HTTP DELETE requests to /api/v1/products/{id}.
     * @DeleteMapping marks this method to handle DELETE requests for a specific resource.
     * @param id The product ID to be deleted, captured from the URL path.
     * @return A response with a 204 No Content status to indicate successful deletion.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id){
        productRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}