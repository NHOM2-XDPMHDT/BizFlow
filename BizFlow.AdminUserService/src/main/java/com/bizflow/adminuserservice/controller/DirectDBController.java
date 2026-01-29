package com.bizflow.adminuserservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bizflow.adminuserservice.entity.Branch;
import com.bizflow.adminuserservice.entity.User;
import com.bizflow.adminuserservice.repository.BranchRepository;
import com.bizflow.adminuserservice.repository.UserRepository;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class DirectDBController {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    public DirectDBController(UserRepository userRepository, BranchRepository branchRepository) {
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

    @GetMapping("/users/staff-count")
    public ResponseEntity<Long> getStaffCount() {
        return ResponseEntity.ok(userRepository.countStaff());
    }

    @GetMapping("/users/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentUsers(@RequestParam(defaultValue = "10") int limit) {
        List<User> users = userRepository.findRecentUsers();
        
        List<Map<String, Object>> result = users.stream()
                .limit(limit)
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("fullName", user.getFullName());
                    map.put("email", user.getEmail());
                    map.put("role", user.getRole());
                    map.put("branchId", user.getBranchId());
                    map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
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
                    map.put("active", branch.getActive());
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/branches/count")
    public ResponseEntity<Long> getBranchesCount() {
        return ResponseEntity.ok(branchRepository.count());
    }
}
