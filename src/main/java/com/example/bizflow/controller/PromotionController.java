package com.example.bizflow.controller;

import com.example.bizflow.dto.PromotionDTO;
import com.example.bizflow.service.PromotionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    /* ================== QUERY (EMPLOYEE + OWNER + ADMIN) ================== */

    // GET /api/promotions
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
    public ResponseEntity<List<PromotionDTO>> getAllPromotions() {
        try {
            return ResponseEntity.ok(promotionService.getAllPromotions());
        } catch (RuntimeException ex) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    // GET /api/promotions/active
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
    public ResponseEntity<List<PromotionDTO>> getActivePromotions() {
        try {
            return ResponseEntity.ok(promotionService.getActivePromotions());
        } catch (RuntimeException ex) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    // GET /api/promotions/code/{code}
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
    public ResponseEntity<PromotionDTO> getByCode(@PathVariable String code) {
        PromotionDTO dto = promotionService.getPromotionByCode(code);
        return dto != null
                ? ResponseEntity.ok(dto)
                : ResponseEntity.notFound().build();
    }

    /* ================== COMMAND (OWNER + ADMIN) ================== */

    // POST /api/promotions
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PromotionDTO> createPromotion(@RequestBody PromotionDTO dto) {
        PromotionDTO created = promotionService.createPromotion(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /api/promotions/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PromotionDTO> updatePromotion(
            @PathVariable Long id,
            @RequestBody PromotionDTO dto
    ) {
        PromotionDTO updated = promotionService.updatePromotion(id, dto);
        return ResponseEntity.ok(updated);
    }

    // PATCH /api/promotions/{id}/deactivate
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deactivatePromotion(@PathVariable Long id) {
        promotionService.deactivatePromotion(id);
        return ResponseEntity.noContent().build();
    }
}

