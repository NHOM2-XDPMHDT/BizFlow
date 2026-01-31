package com.bizflow.adminhomeservice.repository;

import com.bizflow.adminhomeservice.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    
    @Query("SELECT COUNT(b) FROM Branch b WHERE b.active = true")
    long countActive();
}
