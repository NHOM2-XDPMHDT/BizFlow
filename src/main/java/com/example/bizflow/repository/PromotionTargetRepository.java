package com.example.bizflow.repository;

import com.example.bizflow.entity.PromotionTarget;
import com.example.bizflow.entity.PromotionTarget.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionTargetRepository extends JpaRepository<PromotionTarget, Long> {

    List<PromotionTarget> findByPromotion_Id(Long promotionId);

    List<PromotionTarget> findByTargetTypeAndTargetId(TargetType targetType, Long targetId);
}
