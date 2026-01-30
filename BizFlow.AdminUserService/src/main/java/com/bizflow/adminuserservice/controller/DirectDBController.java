package com.bizflow.adminuserservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bizflow.adminuserservice.entity.AdminUser;
import com.bizflow.adminuserservice.entity.Branch;
import com.bizflow.adminuserservice.repository.AdminUserRepository;
import com.bizflow.adminuserservice.repository.BranchRepository;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8200"}, allowCredentials = "true")
public class DirectDBController {

    private final AdminUserRepository userRepository;
    private final BranchRepository branchRepository;

    public DirectDBController(AdminUserRepository userRepository, BranchRepository branchRepository) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "admin-user-service", "database", "bizflow_auth_db"));
    }

    @GetMapping("/users/count")
    public ResponseEntity<Long> getUsersCount() {
        return ResponseEntity.ok(userRepository.count());
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<AdminUser> users = userRepository.findAll();
        
        List<Map<String, Object>> result = users.stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("fullName", user.getFullName());
                    map.put("email", user.getEmail());
                    map.put("role", user.getRole());
                    map.put("branchId", user.getBranch() != null ? user.getBranch().getId() : null);
                    map.put("branchName", user.getBranch() != null ? user.getBranch().getName() : null);
                    map.put("enabled", user.getEnabled());
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("fullName", user.getFullName());
                    map.put("email", user.getEmail());
                    map.put("phoneNumber", user.getPhoneNumber());
                    map.put("role", user.getRole());
                    map.put("branchId", user.getBranch() != null ? user.getBranch().getId() : null);
                    map.put("branchName", user.getBranch() != null ? user.getBranch().getName() : null);
                    map.put("enabled", user.getEnabled());
                    map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
                    map.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
                    map.put("note", user.getNote());
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/staff-count")
    public ResponseEntity<Long> getStaffCount() {
        return ResponseEntity.ok(userRepository.countStaff());
    }

    @GetMapping("/users/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentUsers(@RequestParam(defaultValue = "10") int limit) {
        List<AdminUser> users = userRepository.findRecentUsers();
        
        List<Map<String, Object>> result = users.stream()
                .limit(limit)
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("fullName", user.getFullName());
                    map.put("email", user.getEmail());
                    map.put("role", user.getRole());
                    map.put("branchId", user.getBranch() != null ? user.getBranch().getId() : null);
                    map.put("branchName", user.getBranch() != null ? user.getBranch().getName() : null);
                    map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
                    map.put("enabled", user.getEnabled());
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/branches")
    public ResponseEntity<List<Map<String, Object>>> getBranches() {
        List<Branch> branches = branchRepository.findAll();
        
        List<Map<String, Object>> result = branches.stream()
                .map(branch -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", branch.getId());
                    map.put("name", branch.getName());
                    map.put("address", branch.getAddress());
                    map.put("email", branch.getEmail());
                    map.put("phone", branch.getPhone());
                    map.put("active", branch.getActive() != null ? branch.getActive() : false);
                    
                    // Get owner name if ownerId exists
                    String ownerName = "-";
                    if (branch.getOwnerId() != null) {
                        ownerName = userRepository.findById(branch.getOwnerId())
                                .map(AdminUser::getFullName)
                                .orElse("-");
                    }
                    map.put("ownerName", ownerName);
                    
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/branches/count")
    public ResponseEntity<Long> getBranchesCount() {
        return ResponseEntity.ok(branchRepository.count());
    }

    @GetMapping("/branches/count-active")
    public ResponseEntity<Long> getActiveBranchesCount() {
        return ResponseEntity.ok(branchRepository.countActive());
    }
}
