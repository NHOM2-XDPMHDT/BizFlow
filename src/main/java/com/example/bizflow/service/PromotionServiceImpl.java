package com.example.bizflow.service;

import com.example.bizflow.dto.BundleItemDTO;
import com.example.bizflow.dto.PromotionDTO;
import com.example.bizflow.dto.PromotionTargetDTO;
import com.example.bizflow.entity.BundleItem;
import com.example.bizflow.entity.Promotion;
import com.example.bizflow.entity.PromotionTarget;
import com.example.bizflow.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    /* ================== QUERY ================== */

    @Override
    public List<PromotionDTO> getAllPromotions() {
        return promotionRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromotionDTO> getActivePromotions() {
        return promotionRepository.findActivePromotions(LocalDateTime.now())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PromotionDTO getPromotionByCode(String code) {
        return promotionRepository.findByCode(code)
                .map(this::toDTO)
                .orElse(null);
    }

    /* ================== COMMAND ================== */

    @Override
    public PromotionDTO createPromotion(PromotionDTO dto) {
        Promotion promotion = new Promotion();
        promotion.setTargets(new ArrayList<>());
        promotion.setBundleItems(new ArrayList<>());

        applyDTOToEntity(dto, promotion);
        return toDTO(promotionRepository.save(promotion));
    }

    @Override
    public PromotionDTO updatePromotion(Long id, PromotionDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Promotion id must not be null");
        }

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));

        if (promotion.getTargets() == null) {
            promotion.setTargets(new ArrayList<>());
        } else {
            promotion.getTargets().clear();
        }

        if (promotion.getBundleItems() == null) {
            promotion.setBundleItems(new ArrayList<>());
        } else {
            promotion.getBundleItems().clear();
        }

        applyDTOToEntity(dto, promotion);
        return toDTO(promotionRepository.save(promotion));
    }

    @Override
    public void deactivatePromotion(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Promotion id must not be null");
        }

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));

        promotion.setActive(false);
        promotionRepository.save(promotion);
    }

    /* ================== MAPPING ================== */

    private PromotionDTO toDTO(Promotion promotion) {
        PromotionDTO dto = new PromotionDTO();
        dto.setId(promotion.getId());
        dto.setCode(promotion.getCode());
        dto.setName(promotion.getName());
        dto.setDescription(promotion.getDescription());
        dto.setDiscountType(normalizeDiscountType(promotion));
        dto.setDiscountValue(promotion.getDiscountValue());
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        dto.setActive(promotion.getActive());

        try {
            List<PromotionTarget> targets = promotion.getTargets();
            dto.setTargets(
                    targets == null
                            ? Collections.emptyList()
                            : targets.stream()
                                .map(this::toTargetDTO)
                                .collect(Collectors.toList())
            );
        } catch (RuntimeException ex) {
            dto.setTargets(Collections.emptyList());
        }

        try {
            List<BundleItem> bundleItems = promotion.getBundleItems();
            dto.setBundleItems(
                    bundleItems == null
                            ? Collections.emptyList()
                            : bundleItems.stream()
                                .map(this::toBundleItemDTO)
                                .collect(Collectors.toList())
            );
        } catch (RuntimeException ex) {
            dto.setBundleItems(Collections.emptyList());
        }

        return dto;
    }

    private PromotionTargetDTO toTargetDTO(PromotionTarget target) {
        PromotionTargetDTO dto = new PromotionTargetDTO();
        dto.setId(target.getId());
        dto.setTargetType(target.getTargetType());
        dto.setTargetId(target.getTargetId());
        return dto;
    }

    private BundleItemDTO toBundleItemDTO(BundleItem item) {
        BundleItemDTO dto = new BundleItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setQuantity(item.getQuantity());
        return dto;
    }

    private void applyDTOToEntity(PromotionDTO dto, Promotion promotion) {
        promotion.setCode(dto.getCode());
        promotion.setName(dto.getName());
        promotion.setDescription(dto.getDescription());
        if (dto.getDiscountType() != null) {
            promotion.setDiscountType(dto.getDiscountType());
            promotion.setPromotionType(mapPromotionTypeFromDiscount(dto.getDiscountType()));
        }
        promotion.setDiscountValue(dto.getDiscountValue());
        promotion.setStartDate(dto.getStartDate());
        promotion.setEndDate(dto.getEndDate());
        promotion.setActive(dto.getActive() != null ? dto.getActive() : true);

        if (dto.getTargets() != null) {
            if (promotion.getTargets() == null) {
                promotion.setTargets(new ArrayList<>());
            }
            for (PromotionTargetDTO tdto : dto.getTargets()) {
                PromotionTarget target = new PromotionTarget();
                target.setTargetType(tdto.getTargetType());
                target.setTargetId(tdto.getTargetId());
                target.setPromotion(promotion);
                promotion.getTargets().add(target);
            }
        }

        if (dto.getBundleItems() != null) {
            if (promotion.getBundleItems() == null) {
                promotion.setBundleItems(new ArrayList<>());
            }
            for (BundleItemDTO bdto : dto.getBundleItems()) {
                BundleItem item = new BundleItem();
                item.setProductId(bdto.getProductId());
                item.setQuantity(bdto.getQuantity());
                item.setPromotion(promotion);
                promotion.getBundleItems().add(item);
            }
        }
    }

    private Promotion.DiscountType normalizeDiscountType(Promotion promotion) {
        Promotion.DiscountType raw = promotion.getDiscountType();
        String promotionType = promotion.getPromotionType();

        Promotion.DiscountType fromPromotionType = mapPromotionType(promotionType);
        if (fromPromotionType != null) {
            if (raw == null || raw == Promotion.DiscountType.BUNDLE || raw == Promotion.DiscountType.FREE_GIFT
                    || raw == Promotion.DiscountType.FIXED_AMOUNT) {
                return fromPromotionType;
            }
        }

        return mapDiscountType(raw);
    }

    private Promotion.DiscountType mapDiscountType(Promotion.DiscountType raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case FIXED_AMOUNT -> Promotion.DiscountType.FIXED;
            case FREE_GIFT -> Promotion.DiscountType.BUNDLE;
            default -> raw;
        };
    }

    private Promotion.DiscountType mapPromotionType(String promotionType) {
        if (promotionType == null || promotionType.isBlank()) {
            return null;
        }
        return switch (promotionType) {
            case "PERCENT" -> Promotion.DiscountType.PERCENT;
            case "FIXED_AMOUNT", "FIXED" -> Promotion.DiscountType.FIXED;
            case "FREE_GIFT", "BUNDLE" -> Promotion.DiscountType.BUNDLE;
            default -> null;
        };
    }

    private String mapPromotionTypeFromDiscount(Promotion.DiscountType discountType) {
        if (discountType == null) {
            return null;
        }
        return switch (discountType) {
            case PERCENT -> "PERCENT";
            case FIXED, FIXED_AMOUNT -> "FIXED_AMOUNT";
            case FREE_GIFT -> "FREE_GIFT";
            case BUNDLE -> "BUNDLE";
        };
    }
}


