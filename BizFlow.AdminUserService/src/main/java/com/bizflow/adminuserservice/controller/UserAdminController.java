package com.bizflow.adminuserservice.controller;

import com.bizflow.adminuserservice.entity.AdminUser;
import com.bizflow.adminuserservice.entity.Branch;
import com.bizflow.adminuserservice.repository.AdminUserRepository;
import com.bizflow.adminuserservice.repository.BranchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class UserAdminController {

    private final AdminUserRepository userRepository;
    private final BranchRepository branchRepository;

    public UserAdminController(AdminUserRepository userRepository, BranchRepository branchRepository) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "admin-user-service"));
    }

    @GetMapping("/users/count")
    public ResponseEntity<Long> getUsersCount() {
        return ResponseEntity.ok(userRepository.count());
    }

    @GetMapping("/users/count-by-role")
    public ResponseEntity<Long> getUsersCountByRole(@RequestParam String role) {
        long count = userRepository.findAll().stream()
                .filter(u -> role.equalsIgnoreCase(u.getRole()))
                .count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/users/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentUsers(@RequestParam(defaultValue = "5") int limit) {
        List<AdminUser> users = userRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        List<Map<String, Object>> result = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("fullName", user.getFullName());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole());
            userMap.put("enabled", user.getEnabled());
            userMap.put("createdAt", user.getCreatedAt());
            return userMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/branches/count")
    public ResponseEntity<Long> getBranchesCount() {
        return ResponseEntity.ok(branchRepository.count());
    }

    @GetMapping("/branches/count-active")
    public ResponseEntity<Long> getActiveBranchesCount() {
        long count = branchRepository.findAll().stream()
                .filter(b -> b.getActive() != null && b.getActive())
                .count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/branches")
    public ResponseEntity<List<Map<String, Object>>> getBranches() {
        List<Branch> branches = branchRepository.findAll();

        List<Map<String, Object>> result = branches.stream().map(branch -> {
            Map<String, Object> branchMap = new HashMap<>();
            branchMap.put("id", branch.getId());
            branchMap.put("name", branch.getName());
            branchMap.put("address", branch.getAddress());
            branchMap.put("active", branch.getActive());
            branchMap.put("phone", branch.getPhone());
            branchMap.put("email", branch.getEmail());
            
            // Get owner name if exists
            if (branch.getOwnerId() != null) {
                userRepository.findById(branch.getOwnerId()).ifPresent(owner -> {
                    branchMap.put("ownerName", owner.getFullName());
                });
            } else {
                branchMap.put("ownerName", null);
            }
            
            return branchMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/customers/count")
    public ResponseEntity<Long> getCustomersCount() {
        // For now, return 0 as customers might be in different service
        // You can update this when customer service is integrated
        return ResponseEntity.ok(0L);
    }
}
