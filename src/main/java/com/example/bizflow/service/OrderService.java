package com.example.bizflow.service;

import com.example.bizflow.dto.OrderItemResponse;
import com.example.bizflow.dto.OrderResponse;
import com.example.bizflow.dto.OrderSummaryResponse;
import com.example.bizflow.entity.Order;
import com.example.bizflow.entity.OrderItem;
import com.example.bizflow.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final String invoiceBranchPrefix;

    public OrderService(OrderRepository orderRepository,
                        @Value("${app.invoice.branch-prefix:TC}") String invoiceBranchPrefix) {
        this.orderRepository = orderRepository;
        this.invoiceBranchPrefix = invoiceBranchPrefix;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllWithDetails()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getAllOrderSummaries() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .peek(this::ensureInvoiceNumber)
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> searchPaidOrders(String invoiceNumber,
                                                       String customerPhone,
                                                       LocalDateTime fromDate,
                                                       LocalDateTime toDate) {
        return orderRepository.searchOrders(
                        normalizeOptional(invoiceNumber),
                        fromDate,
                        toDate
                )
                .stream()
                .peek(this::ensureInvoiceNumber)
                .filter(order -> "PAID".equalsIgnoreCase(order.getStatus()))
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> searchPaidOrdersByKeyword(String keyword,
                                                                LocalDateTime fromDate,
                                                                LocalDateTime toDate) {
        return orderRepository.searchOrders(
                        normalizeOptional(keyword),
                        fromDate,
                        toDate
                )
                .stream()
                .peek(this::ensureInvoiceNumber)
                .filter(order -> "PAID".equalsIgnoreCase(order.getStatus()))
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrderDetail(Long id) {
        return orderRepository.findByIdWithDetails(id)
                .map(order -> {
                    ensureInvoiceNumber(order);
                    return order;
                })
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrderByInvoiceNumber(String invoiceNumber) {
        return orderRepository.findByInvoiceNumber(invoiceNumber).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getCustomerOrderHistory(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .peek(this::ensureInvoiceNumber)
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String generateInvoiceNumber(boolean isReturn) {
        return generateInvoiceNumberForDate(LocalDate.now(), isReturn);
    }

    public String generateInvoiceNumberForDate(LocalDate date, boolean isReturn) {
        return generateInvoiceNumberForDate(date, isReturn ? "RETURN" : null);
    }

    public String generateInvoiceNumberForDate(LocalDate date, String orderType) {
        String yymmdd = date.format(DateTimeFormatter.ofPattern("yyMMdd"));
        String basePrefix = invoiceBranchPrefix + "-" + yymmdd;
        int nextSeq = 1;
        Optional<Order> latest = orderRepository.findTopByInvoiceNumberStartingWithOrderByInvoiceNumberDesc(basePrefix);
        if (latest.isPresent() && latest.get().getInvoiceNumber() != null) {
            String raw = latest.get().getInvoiceNumber();
            int parsed = parseSequence(raw, basePrefix);
            if (parsed > 0) {
                nextSeq = parsed + 1;
            }
        }
        String number = String.format("%s%05d", basePrefix, nextSeq);
        String suffix = resolveInvoiceSuffix(orderType);
        if (suffix != null) {
            number += suffix;
        }
        return number;
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(this::mapItem)
                .collect(Collectors.toList());

        String userName = order.getUser() != null ? order.getUser().getFullName() : null;
        String customerPhone = order.getCustomer() != null ? order.getCustomer().getPhone() : null;

        return new OrderResponse(
                order.getId(),
                order.getUser() != null ? order.getUser().getId() : null,
                userName,
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                order.getCustomer() != null ? order.getCustomer().getName() : null,
                customerPhone,
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getInvoiceNumber(),
                order.getStatus(),
                userName,
                userName,
                itemResponses,
                order.getNote()
        );
    }

    private OrderItemResponse mapItem(OrderItem item) {
        BigDecimal lineTotal = item.getPrice() != null && item.getQuantity() != null
                ? item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                : BigDecimal.ZERO;

        String productCode = item.getProduct() != null ? item.getProduct().getCode() : null;
        String barcode = item.getProduct() != null ? item.getProduct().getBarcode() : null;
        String unit = item.getProduct() != null ? item.getProduct().getUnit() : null;

        return new OrderItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getQuantity(),
                item.getPrice(),
                lineTotal,
                productCode,
                barcode,
                unit,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    private OrderSummaryResponse mapToSummary(Order order) {
        int itemCount = order.getItems() != null ? order.getItems().size() : 0;
        String userName = order.getUser() != null ? order.getUser().getFullName() : null;
        String customerPhone = order.getCustomer() != null ? order.getCustomer().getPhone() : null;
        return new OrderSummaryResponse(
                order.getId(),
                order.getUser() != null ? order.getUser().getId() : null,
                userName,
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                order.getCustomer() != null ? order.getCustomer().getName() : null,
                customerPhone,
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemCount,
                order.getInvoiceNumber(),
                order.getStatus(),
                userName,
                userName,
                order.getNote()
        );
    }

    private int parseSequence(String invoiceNumber, String basePrefix) {
        if (invoiceNumber == null || basePrefix == null) {
            return 0;
        }
        if (!invoiceNumber.startsWith(basePrefix)) {
            return 0;
        }
        int start = basePrefix.length();
        String trimmed = invoiceNumber;
        if (trimmed.endsWith("TH") || trimmed.endsWith("ĐH")) {
            trimmed = trimmed.substring(0, trimmed.length() - 2);
        }
        if (trimmed.length() < start + 5) {
            return 0;
        }
        String digits = trimmed.substring(start, start + 5);
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void ensureInvoiceNumber(Order order) {
        if (order == null) {
            return;
        }
        String current = order.getInvoiceNumber();
        if (current != null && !current.trim().isEmpty()) {
            return;
        }
        LocalDate date = order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : LocalDate.now();
        String orderType = order.getOrderType();
        if (orderType == null && Boolean.TRUE.equals(order.getReturnOrder())) {
            orderType = "RETURN";
        }
        order.setInvoiceNumber(generateInvoiceNumberForDate(date, orderType));
        if (order.getStatus() == null || order.getStatus().trim().isEmpty()) {
            boolean isReturn = "RETURN".equalsIgnoreCase(orderType);
            order.setStatus(isReturn ? "RETURNED" : "PAID");
        }
        orderRepository.save(order);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveInvoiceSuffix(String orderType) {
        if (orderType == null) {
            return null;
        }
        switch (orderType.toUpperCase()) {
            case "RETURN":
                return "TH";
            case "EXCHANGE":
                return "ĐH";
            default:
                return null;
        }
    }
}
