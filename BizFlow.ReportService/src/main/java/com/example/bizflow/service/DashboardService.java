package com.example.bizflow.service;

import com.example.bizflow.config.RoutingDataSource;
import com.example.bizflow.dto.AdminDashboardSummary;
import com.example.bizflow.dto.BranchSummary;
import com.example.bizflow.dto.OwnerDashboardSummary;
import com.example.bizflow.dto.RecentUserSummary;
import com.example.bizflow.dto.UserDetailDto;
import com.example.bizflow.dto.UserSearchItem;
import com.example.bizflow.entity.Branch;
import com.example.bizflow.entity.User;
import com.example.bizflow.repository.BranchRepository;
import com.example.bizflow.repository.CustomerRepository;
import com.example.bizflow.repository.ProductRepository;
import com.example.bizflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final TransactionTemplate requiresNewTx;

    public DashboardService(UserRepository userRepository,
            BranchRepository branchRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            PlatformTransactionManager multiTransactionManager) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;

        TransactionTemplate template = new TransactionTemplate(
                Objects.requireNonNull(multiTransactionManager, "multiTransactionManager must not be null"));
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(true);
        this.requiresNewTx = template;
    }

    public OwnerDashboardSummary getOwnerSummary() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // IMPORTANT: không đổi database trong cùng 1 transaction vì connection đã được
        // lấy từ lần query đầu tiên.
        // Mỗi database query được chạy trong một transaction REQUIRES_NEW riêng.
        long[] authCounts = runInDatabase("auth", () -> new long[] {
                userRepository.count(),
                userRepository.countByRole("EMPLOYEE"),
                userRepository.countByRole("MANAGER"),
                branchRepository.count()
        });

        long totalCustomers = runInDatabase("customer", customerRepository::count);
        long totalProducts = runInDatabase("catalog", productRepository::count);

        long totalUsers = authCounts[0];
        long totalEmployees = authCounts[1];
        long totalManagers = authCounts[2];
        long totalBranches = authCounts[3];

        return new OwnerDashboardSummary(
                timestamp,
                totalUsers,
                totalEmployees,
                totalManagers,
                totalCustomers,
                totalProducts,
                totalBranches);
    }

    public AdminDashboardSummary getAdminSummary() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        long[] authCounts = runInDatabase("auth", () -> new long[] {
                userRepository.count(),
                userRepository.countByRole("EMPLOYEE"),
                userRepository.countByRole("MANAGER"),
                branchRepository.count(),
                branchRepository.countByIsActive(true)
        });

        long totalProducts = runInDatabase("catalog", productRepository::count);
        long totalCustomers = runInDatabase("customer", customerRepository::count);

        long totalUsers = authCounts[0];
        long totalEmployees = authCounts[1];
        long totalManagers = authCounts[2];
        long totalBranches = authCounts[3];
        long activeBranches = authCounts[4];

        return new AdminDashboardSummary(
                timestamp,
                totalUsers,
                totalEmployees,
                totalManagers,
                totalBranches,
                activeBranches,
                totalProducts,
                totalCustomers);
    }

    public List<RecentUserSummary> getRecentUsers() {
        // Lấy 5 user mới nhất từ auth database
        List<User> users = runInDatabase("auth", userRepository::findAllByOrderByCreatedAtDesc);

        return users.stream()
                .limit(5)
                .map(this::toRecentUserSummary)
                .collect(Collectors.toList());
    }

    public List<BranchSummary> getBranchSummaries() {
        // Lấy tất cả branches và users từ auth database
        Map<String, Object> authData = runInDatabase("auth", () -> {
            Map<String, Object> result = new HashMap<>();
            result.put("branches", branchRepository.findAll());
            result.put("users", userRepository.findAll());
            return result;
        });

        @SuppressWarnings("unchecked")
        List<Branch> branches = (List<Branch>) authData.get("branches");

        @SuppressWarnings("unchecked")
        List<User> allUsers = (List<User>) authData.get("users");

        // Tạo map để lookup owner information
        Map<Long, User> userMap = new HashMap<>();
        for (User user : allUsers) {
            userMap.put(user.getId(), user);
        }

        return branches.stream()
                .map(branch -> toBranchSummary(branch, userMap))
                .collect(Collectors.toList());
    }

    public List<UserSearchItem> getAllUsersForSearch() {
        List<User> users = runInDatabase("auth", userRepository::findAll);
        return users.stream()
                .map(this::toUserSearchItem)
                .collect(Collectors.toList());
    }

    public Optional<UserDetailDto> getUserDetail(Long userId) {
        return runInDatabase("auth", () -> userRepository.findById(userId).map(this::toUserDetailDto));
    }

    private <T> T runInDatabase(String databaseKey, Supplier<T> action) {
        // IMPORTANT: phải set routing key trước khi transaction bắt đầu,
        // vì DataSourceTransactionManager sẽ acquire connection ngay khi begin.
        RoutingDataSource.setCurrentDatabase(databaseKey);
        try {
            return requiresNewTx.execute(status -> action.get());
        } finally {
            RoutingDataSource.clearCurrentDatabase();
        }
    }

    private RecentUserSummary toRecentUserSummary(User user) {
        String createdAt = user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;
        String fullName = user.getFullName() != null ? user.getFullName() : "-";
        String role = user.getRole() != null ? user.getRole() : "-";
        return new RecentUserSummary(
                user.getId(),
                user.getUsername(),
                fullName,
                role,
                createdAt);
    }

    private UserSearchItem toUserSearchItem(User user) {
        String fullName = user.getFullName() != null ? user.getFullName() : "-";
        String role = user.getRole() != null ? user.getRole() : "-";
        return new UserSearchItem(
                user.getId(),
                user.getUsername(),
                fullName,
                user.getEmail(),
                role);
    }

    private UserDetailDto toUserDetailDto(User user) {
        String createdAt = user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;

        String branchName = null;
        if (user.getBranchId() != null) {
            branchName = branchRepository.findById(user.getBranchId())
                    .map(Branch::getName)
                    .orElse(null);
        }

        String fullName = user.getFullName() != null ? user.getFullName() : "-";
        String role = user.getRole() != null ? user.getRole() : "-";

        return new UserDetailDto(
                user.getId(),
                user.getUsername(),
                fullName,
                user.getEmail(),
                user.getPhoneNumber(),
                role,
                user.getEnabled() != null ? user.getEnabled() : Boolean.FALSE,
                branchName,
                createdAt);
    }

    private BranchSummary toBranchSummary(Branch branch, Map<Long, User> userMap) {
        String ownerName = "-";
        if (branch.getOwnerId() != null) {
            User owner = userMap.get(branch.getOwnerId());
            if (owner != null) {
                ownerName = owner.getFullName();
                if (ownerName == null || ownerName.isBlank()) {
                    ownerName = owner.getUsername();
                }
            }
        }
        return new BranchSummary(
                branch.getId(),
                branch.getName(),
                ownerName,
                branch.getIsActive(),
                branch.getAddress());
    }
}
