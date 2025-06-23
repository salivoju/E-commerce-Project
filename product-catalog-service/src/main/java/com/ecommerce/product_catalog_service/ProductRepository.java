package com.ecommerce.product_catalog_service;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * This is our Repository interface.
 * @Repository tells Spring that this is a component that handles data access.
 * By extending JpaRepository<Product, Long>, we get a whole set of CRUD methods
 * (like save(), findById(), findAll(), delete()) for our Product entity for free.
 * The 'Product' is the entity type, and 'Long' is the type of its primary key (id).
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
//    Optional<Product> findAllById(Long id);
//    Product getReferenceById(Id id);
}
