package com.bizflow.adminproductservice.service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bizflow.adminproductservice.dto.ProductOverviewDto;
import com.bizflow.adminproductservice.entity.ProductInventory;
import com.bizflow.adminproductservice.exception.ProductNotFoundException;
import com.bizflow.adminproductservice.repository.ProductInventoryRepository;
import com.bizflow.adminproductservice.request.ProductStatusUpdateRequest;

@Service
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductInventoryRepository productInventoryRepository;

    public AdminProductServiceImpl(ProductInventoryRepository productInventoryRepository) {
        this.productInventoryRepository = productInventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductOverviewDto> listProducts(String query, Boolean active) {
        return productInventoryRepository.findAll().stream()
                .filter(product -> matchesActive(active, product))
                .filter(product -> matchesQuery(query, product))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductOverviewDto getProduct(Long id) {
        return productInventoryRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    @Transactional
    public ProductOverviewDto updateProductStatus(Long id, ProductStatusUpdateRequest request) {
        ProductInventory inventory = productInventoryRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        inventory.setActive(request.getActive());
        return toDto(productInventoryRepository.save(inventory));
    }

    private ProductOverviewDto toDto(ProductInventory product) {
        return new ProductOverviewDto(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getActive(),
                product.getStock(),
                product.getPrice(),
                product.getUpdatedAt()
        );
    }

    private boolean matchesActive(Boolean active, ProductInventory product) {
        if (active == null) {
            return true;
        }
        return active.equals(product.getActive());
    }

    private boolean matchesQuery(String query, ProductInventory product) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return product.getSku().toLowerCase(Locale.ROOT).contains(normalized)
                || product.getName().toLowerCase(Locale.ROOT).contains(normalized);
    }
}
