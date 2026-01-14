package com.example.bizflow.service;

import com.example.bizflow.dto.AdminDashboardSummary;
import com.example.bizflow.dto.BranchSummary;
import com.example.bizflow.dto.OwnerDashboardSummary;
import com.example.bizflow.dto.RecentUserSummary;
import com.example.bizflow.entity.Branch;
import com.example.bizflow.entity.Role;
import com.example.bizflow.entity.User;
import com.example.bizflow.repository.BranchRepository;
import com.example.bizflow.repository.CustomerRepository;
import com.example.bizflow.repository.ProductRepository;
import com.example.bizflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;

    @Autowired
    public DashboardService(UserRepository userRepository,
                            ProductRepository productRepository,
                            CustomerRepository customerRepository,
                            BranchRepository branchRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
    }

    public OwnerDashboardSummary getOwnerSummary() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        long totalUsers = userRepository.count();
        long totalEmployees = userRepository.countByRole(Role.EMPLOYEE);
        long totalManagers = userRepository.countByRole(Role.MANAGER);
        long totalCustomers = customerRepository.count();
        long totalProducts = productRepository.count();
        long totalBranches = branchRepository.count();

        return new OwnerDashboardSummary(
                timestamp,
                totalUsers,
                totalEmployees,
                totalManagers,
                totalCustomers,
                totalProducts,
                totalBranches
        );
    }

    public AdminDashboardSummary getAdminSummary() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        long totalUsers = userRepository.count();
        long totalEmployees = userRepository.countByRole(Role.EMPLOYEE);
        long totalManagers = userRepository.countByRole(Role.MANAGER);
        long totalBranches = branchRepository.count();
        long activeBranches = branchRepository.countByIsActiveTrue();
        long totalProducts = productRepository.count();
        long totalCustomers = customerRepository.count();

        return new AdminDashboardSummary(
                timestamp,
                totalUsers,
                totalEmployees,
                totalManagers,
                totalBranches,
                activeBranches,
                totalProducts,
                totalCustomers
        );
    }

    public List<RecentUserSummary> getRecentUsers() {
        return userRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toRecentUserSummary)
                .collect(Collectors.toList());
    }

    public List<BranchSummary> getBranchSummaries() {
        return branchRepository.findAll()
                .stream()
                .map(this::toBranchSummary)
                .collect(Collectors.toList());
    }

    private RecentUserSummary toRecentUserSummary(User user) {
        String createdAt = user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;
        String fullName = user.getFullName() != null ? user.getFullName() : "-";
        return new RecentUserSummary(
                user.getId(),
                user.getUsername(),
                fullName,
                user.getRole() != null ? user.getRole().name() : "-",
                createdAt
        );
    }

    private BranchSummary toBranchSummary(Branch branch) {
        String ownerName = "-";
        if (branch.getOwner() != null) {
            ownerName = branch.getOwner().getFullName();
            if (ownerName == null || ownerName.isBlank()) {
                ownerName = branch.getOwner().getUsername();
            }
        }
        return new BranchSummary(
                branch.getId(),
                branch.getName(),
                ownerName,
                branch.getIsActive(),
                branch.getAddress()
        );
    }
}
