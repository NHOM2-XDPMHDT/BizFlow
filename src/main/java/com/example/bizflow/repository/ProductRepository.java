package com.example.bizflow.repository;

import com.example.bizflow.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    java.util.List<Product> findByCategoryId(Long categoryId);
    java.util.Optional<Product> findByCode(String code);
    java.util.List<Product> findByActive(Boolean active);
}
