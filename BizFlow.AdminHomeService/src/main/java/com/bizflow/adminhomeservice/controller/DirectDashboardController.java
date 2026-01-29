package com.bizflow.adminhomeservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DirectDashboardController {

    private final RestTemplate restTemplate;

    @Value("${admin.user.service.url:http://bizflow-admin-user-service:8201}")
    private String userServiceUrl;

    @Value("${admin.product.service.url:http://bizflow-admin-product-service:8204}")
    private String productServiceUrl;

    public DirectDashboardController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "admin-home-service"));
    }

    @GetMapping("/admin-summary")
    public ResponseEntity<Map<String, Object>> getAdminSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            // Get total users count from bizflow_auth_db
            Long totalUsers = restTemplate.getForObject(userServiceUrl + "/api/admin/users/count", Long.class);
            summary.put("totalUsers", totalUsers != null ? totalUsers : 0);
            
            // Get staff count from bizflow_auth_db
            Long staffCount = restTemplate.getForObject(userServiceUrl + "/api/admin/users/staff-count", Long.class);
            summary.put("employeeCount", staffCount != null ? staffCount : 0);
            
            // Get branches count from bizflow_auth_db
            Long branchCount = restTemplate.getForObject(userServiceUrl + "/api/admin/branches/count", Long.class);
            summary.put("branchCount", branchCount != null ? branchCount : 0);
            
            // Get products count from bizflow_catalog_db
            Long productCount = restTemplate.getForObject(productServiceUrl + "/api/admin/products/count", Long.class);
            summary.put("productCount", productCount != null ? productCount : 0);
            
        } catch (Exception e) {
            // Return zeros if services are unavailable
            summary.put("totalUsers", 0);
            summary.put("employeeCount", 0);
            summary.put("branchCount", 0);
            summary.put("productCount", 0);
            summary.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/recent-users")
    public ResponseEntity<List<Map<String, Object>>> getRecentUsers(@RequestParam(defaultValue = "10") int limit) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = restTemplate.getForObject(
                userServiceUrl + "/api/admin/users/recent?limit=" + limit, 
                List.class
            );
            return ResponseEntity.ok(users != null ? users : new ArrayList<>());
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/branches")
    public ResponseEntity<List<Map<String, Object>>> getBranches() {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> branches = restTemplate.getForObject(
                userServiceUrl + "/api/admin/branches", 
                List.class
            );
            return ResponseEntity.ok(branches != null ? branches : new ArrayList<>());
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
}
