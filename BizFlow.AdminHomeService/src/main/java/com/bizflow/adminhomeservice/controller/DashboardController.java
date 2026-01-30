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

            // Get all users to count employees
            List<Map<String, Object>> allUsers = callService(adminUserServiceUrl + "/api/admin/users", List.class);
            int totalEmployees = 0;
            if (allUsers != null) {
                for (Map<String, Object> user : allUsers) {
                    String role = (String) user.get("role");
                    if ("EMPLOYEE".equals(role)) {
                        totalEmployees++;
                    }
                }
            }
            summary.put("totalEmployees", totalEmployees);

            // Get branches - use authentication service
            List branches = callService("http://authentication-service:8086/api/branches", List.class);
            int totalBranches = branches != null ? branches.size() : 0;
            int activeBranches = 0;
            if (branches != null) {
                for (Object branch : branches) {
                    if (branch instanceof Map) {
                        Boolean isActive = (Boolean) ((Map) branch).get("isActive");
                        if (Boolean.TRUE.equals(isActive)) {
                            activeBranches++;
                        }
                    }
                }
            }
            summary.put("totalBranches", totalBranches);
            summary.put("activeBranches", activeBranches);

            // Get products count from admin-product-service
            Integer totalProducts = callService(adminProductServiceUrl + "/api/admin/products/count", Integer.class);
            summary.put("totalProducts", totalProducts != null ? totalProducts : 0);

            // Customers - set to 0 as customer service doesn't have count endpoint
            summary.put("totalCustomers", 0);

        } catch (Exception e) {
            System.err.println("Error in getAdminSummary: " + e.getMessage());
            // Return default values if services are not available
            summary.put("totalUsers", 0);
            summary.put("totalEmployees", 0);
            summary.put("totalBranches", 0);
            summary.put("activeBranches", 0);
            summary.put("totalProducts", 0);
            summary.put("totalCustomers", 0);
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
            // Call authentication-service to get branches
            List<Map<String, Object>> branches = callService(
                "http://authentication-service:8086/api/branches",
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
