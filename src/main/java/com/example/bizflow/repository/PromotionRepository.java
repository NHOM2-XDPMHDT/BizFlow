package com.example.bizflow.repository;

import com.example.bizflow.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCode(String code);

    List<Promotion> findByActiveTrue();

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.active = true
          AND (p.startDate IS NULL OR p.startDate <= :now)
          AND (p.endDate IS NULL OR p.endDate >= :now)
    """)
    List<Promotion> findActivePromotions(LocalDateTime now);
}
