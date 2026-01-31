package com.bizflow.adminhomeservice.repository;

import com.bizflow.adminhomeservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role != 'ADMIN'")
    long countStaff();
    
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findRecentUsers();
}
