package com.example.bizflow.repository;

import com.example.bizflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Count users by role
    long countByRole(String role);

    // Count all users
    @Override
    long count();

    // Find users by role
    List<User> findByRole(String role);

    // Find all users ordered by creation date descending
    List<User> findAllByOrderByCreatedAtDesc();

    // Find top N recent users
    @Query(value = "SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findRecentUsers();
}
