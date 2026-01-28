package com.bizflow.adminuserservice.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bizflow.adminuserservice.dto.AdminUserDto;
import com.bizflow.adminuserservice.request.AdminUserCreationRequest;
import com.bizflow.adminuserservice.request.AdminUserStatusUpdateRequest;
import com.bizflow.adminuserservice.request.BranchStaffCreationRequest;
import com.bizflow.adminuserservice.service.AdminUserService;

@RestController
@RequestMapping("/admin/users")
public class UserAdminController {

    private final AdminUserService adminUserService;

    public UserAdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "admin-user-service"));
    }

    @GetMapping
    public ResponseEntity<List<AdminUserDto>> listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(adminUserService.searchAdminUsers(q, role, enabled, branchId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDto> getUserDetail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(adminUserService.getAdminUserById(id));
    }

    @PostMapping
    public ResponseEntity<AdminUserDto> createUser(@Valid @RequestBody AdminUserCreationRequest payload) {
        AdminUserDto created = adminUserService.createAdminUser(payload);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AdminUserDto> updateStatus(@PathVariable("id") Long id,
                                                     @Valid @RequestBody AdminUserStatusUpdateRequest payload) {
        return ResponseEntity.ok(adminUserService.updateAdminUserStatus(id, payload));
    }

    @PostMapping("/branches/{branchId}/staff")
    public ResponseEntity<AdminUserDto> createBranchStaff(@PathVariable("branchId") Long branchId,
                                                          @Valid @RequestBody BranchStaffCreationRequest payload) {
        AdminUserDto staff = adminUserService.createBranchStaff(branchId, payload);
        return ResponseEntity.status(201).body(staff);
    }
}
