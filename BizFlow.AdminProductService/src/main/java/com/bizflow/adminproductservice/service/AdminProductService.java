package com.bizflow.adminproductservice.service;

import java.util.List;

import com.bizflow.adminproductservice.dto.ProductOverviewDto;
import com.bizflow.adminproductservice.request.ProductStatusUpdateRequest;

public interface AdminProductService {

    List<ProductOverviewDto> listProducts(String query, Boolean active);

    ProductOverviewDto getProduct(Long id);

    ProductOverviewDto updateProductStatus(Long id, ProductStatusUpdateRequest request);
}
