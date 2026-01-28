package com.bizflow.adminorderservice.service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bizflow.adminorderservice.dto.OrderSummaryDto;
import com.bizflow.adminorderservice.entity.OrderRecord;
import com.bizflow.adminorderservice.exception.OrderNotFoundException;
import com.bizflow.adminorderservice.repository.OrderRecordRepository;
import com.bizflow.adminorderservice.request.OrderStatusUpdateRequest;
import com.bizflow.producer.OrderEventProducer;
import com.bizflow.event.PurchaseEvent;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrderServiceImpl.class);
    private final OrderRecordRepository orderRecordRepository;
    private final OrderEventProducer orderEventProducer;

    public AdminOrderServiceImpl(OrderRecordRepository orderRecordRepository, OrderEventProducer orderEventProducer) {
        this.orderRecordRepository = orderRecordRepository;
        this.orderEventProducer = orderEventProducer;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryDto> listOrders(String status, String query) {
        return orderRecordRepository.findAll().stream()
                .filter(order -> matchesStatus(status, order))
                .filter(order -> matchesQuery(query, order))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSummaryDto getOrder(Long id) {
        return orderRecordRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    @Transactional
    public OrderSummaryDto updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        OrderRecord record = orderRecordRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        record.setStatus(request.getStatus().toUpperCase(Locale.ROOT));
        OrderRecord updatedRecord = orderRecordRepository.save(record);
        
        // Publish event to Kafka
        try {
            PurchaseEvent event = new PurchaseEvent();
            event.setOrderId(updatedRecord.getId());
            // Note: customerId is extracted from invoice number pattern or order history
            event.setInvoiceNumber(updatedRecord.getInvoiceNumber());
            if (updatedRecord.getTotalAmount() != null) {
                event.setTotalAmount(new java.math.BigDecimal(updatedRecord.getTotalAmount()));
            }
            event.setStatus(updatedRecord.getStatus());
            if (updatedRecord.getCreatedAt() != null) {
                event.setCreatedAt(java.time.LocalDateTime.ofInstant(updatedRecord.getCreatedAt(), java.time.ZoneId.systemDefault()));
            }
            
            orderEventProducer.publishPurchaseEvent(event);
            logger.info("Published purchase event for order {} with status {}", id, updatedRecord.getStatus());
        } catch (Exception e) {
            logger.warn("Failed to publish Kafka event for order {}", id, e);
            // Don't fail the order status update if Kafka fails
        }
        
        return toDto(updatedRecord);
    }

    private OrderSummaryDto toDto(OrderRecord record) {
        return new OrderSummaryDto(
                record.getId(),
                record.getInvoiceNumber(),
                record.getStatus(),
                record.getCustomerName(),
                record.getTotalAmount(),
                record.getCreatedAt()
        );
    }

    private boolean matchesStatus(String status, OrderRecord record) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return status.equalsIgnoreCase(record.getStatus());
    }

    private boolean matchesQuery(String query, OrderRecord record) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return record.getInvoiceNumber().toLowerCase(Locale.ROOT).contains(normalized) ||
                (record.getCustomerName() != null &&
                        record.getCustomerName().toLowerCase(Locale.ROOT).contains(normalized));
    }
}
