package com.example.bizflow.service;

import com.example.bizflow.dto.PromotionDTO;

import java.util.List;

public interface PromotionService {

    List<PromotionDTO> getAllPromotions();

    List<PromotionDTO> getActivePromotions();

    PromotionDTO getPromotionByCode(String code);

    PromotionDTO createPromotion(PromotionDTO dto);

    PromotionDTO updatePromotion(Long id, PromotionDTO dto);

    void deactivatePromotion(Long id);
}
