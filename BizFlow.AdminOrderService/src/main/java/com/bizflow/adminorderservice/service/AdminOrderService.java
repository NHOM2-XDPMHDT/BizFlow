package com.bizflow.adminorderservice.service;

import java.util.List;

import com.bizflow.adminorderservice.dto.OrderSummaryDto;
import com.bizflow.adminorderservice.request.OrderStatusUpdateRequest;

public interface AdminOrderService {

    List<OrderSummaryDto> listOrders(String status, String query);

    OrderSummaryDto getOrder(Long id);

    OrderSummaryDto updateOrderStatus(Long id, OrderStatusUpdateRequest request);
}
