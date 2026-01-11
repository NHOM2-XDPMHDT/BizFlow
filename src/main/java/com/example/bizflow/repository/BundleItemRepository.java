package com.example.bizflow.repository;

import com.example.bizflow.entity.BundleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BundleItemRepository extends JpaRepository<BundleItem, Long> {

    List<BundleItem> findByPromotion_Id(Long promotionId);

    List<BundleItem> findByProductId(Long productId);
}
