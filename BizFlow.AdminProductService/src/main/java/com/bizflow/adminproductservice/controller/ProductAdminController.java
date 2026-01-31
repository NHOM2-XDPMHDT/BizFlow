package com.bizflow.adminproductservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bizflow.adminproductservice.dto.ProductOverviewDto;
import com.bizflow.adminproductservice.request.ProductStatusUpdateRequest;
import com.bizflow.adminproductservice.service.AdminProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/products")
@CrossOrigin(origins = "*")
public class ProductAdminController {

    private final AdminProductService adminProductService;

    public ProductAdminController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "admin-product-service"));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("API is working!");
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getProductsCount() {
        try {
            long count = (long) adminProductService.listProducts(null, null).size();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(-1L);
        }
    }

    @GetMapping
    public ResponseEntity<List<ProductOverviewDto>> listProducts(@RequestParam(required = false) String q,
                                                                 @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(adminProductService.listProducts(q, active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductOverviewDto> getProduct(@PathVariable("id") Long id) {
        return ResponseEntity.ok(adminProductService.getProduct(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ProductOverviewDto> updateStatus(@PathVariable("id") Long id,
                                                           @Valid @RequestBody ProductStatusUpdateRequest request) {
        return ResponseEntity.ok(adminProductService.updateProductStatus(id, request));
    }
}
