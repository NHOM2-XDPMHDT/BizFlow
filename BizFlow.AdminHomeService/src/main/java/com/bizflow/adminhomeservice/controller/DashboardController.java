package com.bizflow.adminhomeservice.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final RestTemplate restTemplate;

    @Value("${services.admin-user.url:http://admin-user-service:8201}")
    private String adminUserServiceUrl;

    @Value("${services.admin-product.url:http://admin-product-service:8204}")
    private String adminProductServiceUrl;

    @Value("${services.admin-order.url:http://admin-order-service:8203}")
    private String adminOrderServiceUrl;

    public DashboardController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/admin-summary")
    public ResponseEntity<Map<String, Object>> getAdminSummary() {
        Map<String, Object> summary = new HashMap<>();

        try {
            // Get users count from admin-user-service
            Integer totalUsers = callService(adminUserServiceUrl + "/api/admin/users/count", Integer.class);
            summary.put("totalUsers", totalUsers != null ? totalUsers : 0);

            Integer totalEmployees = callService(adminUserServiceUrl + "/api/admin/users/count-by-role?role=EMPLOYEE", Integer.class);
            summary.put("totalEmployees", totalEmployees != null ? totalEmployees : 0);

            // Get branches count
            Integer totalBranches = callService(adminUserServiceUrl + "/api/admin/branches/count", Integer.class);
            summary.put("totalBranches", totalBranches != null ? totalBranches : 0);

            Integer activeBranches = callService(adminUserServiceUrl + "/api/admin/branches/count-active", Integer.class);
            summary.put("activeBranches", activeBranches != null ? activeBranches : 0);

            // Get products count from admin-product-service
            Integer totalProducts = callService(adminProductServiceUrl + "/api/admin/products/count", Integer.class);
            summary.put("totalProducts", totalProducts != null ? totalProducts : 0);

            // Get customers count (assuming from user service)
            Integer totalCustomers = callService(adminUserServiceUrl + "/api/admin/customers/count", Integer.class);
            summary.put("totalCustomers", totalCustomers != null ? totalCustomers : 0);

        } catch (Exception e) {
            // Return default values if services are not available
            summary.put("totalUsers", 6);
            summary.put("totalEmployees", 3);
            summary.put("totalBranches", 2);
            summary.put("activeBranches", 2);
            summary.put("totalProducts", 155);
            summary.put("totalCustomers", 8);
        }

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/recent-users")
    public ResponseEntity<List<Map<String, Object>>> getRecentUsers() {
        try {
            // Call admin-user-service to get recent users
            List<Map<String, Object>> users = callService(
                adminUserServiceUrl + "/api/admin/users/recent?limit=5",
                List.class
            );
            return ResponseEntity.ok(users != null ? users : Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/branches")
    public ResponseEntity<List<Map<String, Object>>> getBranches() {
        try {
            // Call admin-user-service to get branches
            List<Map<String, Object>> branches = callService(
                adminUserServiceUrl + "/api/admin/branches",
                List.class
            );
            return ResponseEntity.ok(branches != null ? branches : Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    private <T> T callService(String url, Class<T> responseType) {
        try {
            return restTemplate.getForObject(url, responseType);
        } catch (Exception e) {
            System.err.println("Error calling service: " + url + " - " + e.getMessage());
            return null;
        }
    }
}
