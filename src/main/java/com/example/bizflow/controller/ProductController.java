package com.example.bizflow.controller;

import com.example.bizflow.entity.Product;
import com.example.bizflow.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductRepository productRepository;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
    public ResponseEntity<?> getAllProducts() {
        try {
            List<Product> products = productRepository.findByStatus("active");
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            try {
                List<Product> products = productRepository.findAll();
                return ResponseEntity.ok(products);
            } catch (Exception fallback) {
                return ResponseEntity.status(500).body("Error fetching products: " + e.getMessage());
            }
        }
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
    public ResponseEntity<?> getProductById(@PathVariable @NonNull Long id) {
        try {
            return productRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching product: " + e.getMessage());
        }
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> createProduct(@RequestBody @NonNull Product product) {
        try {
            Product saved = productRepository.save(product);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error creating product: " + e.getMessage());
        }
    }
}
