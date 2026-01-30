package com.bizflow.adminuserservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.bizflow.adminuserservice.entity.AdminUser;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByUsername(String username);

    Optional<AdminUser> findByEmail(String email);
    
    @Query("SELECT COUNT(u) FROM AdminUser u WHERE u.role IN ('EMPLOYEE', 'MANAGER', 'OWNER')")
    long countStaff();
    
    @Query("SELECT u FROM AdminUser u ORDER BY u.createdAt DESC")
    List<AdminUser> findRecentUsers();
}
