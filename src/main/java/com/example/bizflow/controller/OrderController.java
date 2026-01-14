package com.example.bizflow.controller;

import com.example.bizflow.dto.CreateOrderRequest;
import com.example.bizflow.dto.CreateOrderResponse;
import com.example.bizflow.dto.OrderItemRequest;
import com.example.bizflow.dto.OrderResponse;
import com.example.bizflow.dto.OrderSummaryResponse;
import com.example.bizflow.entity.Customer;
import com.example.bizflow.entity.Order;
import com.example.bizflow.entity.OrderItem;
import com.example.bizflow.entity.Payment;
import com.example.bizflow.entity.Product;
import com.example.bizflow.entity.User;
import com.example.bizflow.repository.CustomerRepository;
import com.example.bizflow.repository.OrderItemRepository;
import com.example.bizflow.repository.OrderRepository;
import com.example.bizflow.repository.PaymentRepository;
import com.example.bizflow.repository.ProductRepository;
import com.example.bizflow.repository.UserRepository;
import com.example.bizflow.service.OrderService;
import com.example.bizflow.service.PointService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final PointService pointService;

    public OrderController(OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           PaymentRepository paymentRepository,
                           ProductRepository productRepository,
                           CustomerRepository customerRepository,
                           UserRepository userRepository,
                           OrderService orderService,
                           PointService pointService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.pointService = pointService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/summary")
    public ResponseEntity<List<OrderSummaryResponse>> getOrderSummaries() {
        return ResponseEntity.ok(orderService.getAllOrderSummaries());
    }

    @GetMapping("/returns/search")
    public ResponseEntity<List<OrderSummaryResponse>> searchPaidOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        LocalDateTime from = null;
        LocalDateTime to = null;
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            from = LocalDate.parse(fromDate.trim()).atStartOfDay();
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            to = LocalDate.parse(toDate.trim()).atTime(23, 59, 59);
        }
        return ResponseEntity.ok(orderService.searchPaidOrdersByKeyword(keyword, from, to));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable Long orderId) {
        Optional<OrderResponse> order = orderService.getOrderDetail(orderId);
        return order.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<?> getOrderByInvoiceNumber(@PathVariable String invoiceNumber) {
        return orderService.getOrderByInvoiceNumber(invoiceNumber)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body("Order items are required");
        }

        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId()).orElse(null);
        }

        Customer customer = null;
        if (request.getCustomerId() != null && request.getCustomerId() > 0) {
            customer = customerRepository.findById(request.getCustomerId()).orElse(null);
        }

        String orderType = request.getOrderType();
        boolean isReturn = Boolean.TRUE.equals(request.getReturnOrder())
                || "RETURN".equalsIgnoreCase(orderType);
        boolean isExchange = "EXCHANGE".equalsIgnoreCase(orderType);
        if (isReturn && orderType == null) {
            orderType = "RETURN";
        }
        if (isExchange && orderType == null) {
            orderType = "EXCHANGE";
        }
        boolean paid = Boolean.TRUE.equals(request.getPaid());

        Order order = new Order();
        order.setUser(user);
        order.setCustomer(customer);
        order.setReturnOrder(isReturn);
        order.setOrderType(orderType);
        order.setStatus(isReturn ? "RETURNED" : (paid ? "PAID" : "UNPAID"));
        order.setInvoiceNumber(orderService.generateInvoiceNumberForDate(LocalDate.now(), orderType));

        if (request.getOriginalOrderId() != null) {
            orderRepository.findById(request.getOriginalOrderId()).ifPresent(order::setParentOrder);
        }

        if (isReturn) {
            order.setRefundMethod(request.getRefundMethod());
            order.setReturnReason(request.getReturnReason());
            order.setReturnNote(request.getReturnNote());
        }
        order.setNote(request.getReturnNote());

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest == null || itemRequest.getProductId() == null) {
                return ResponseEntity.badRequest().body("Product id is required.");
            }

            int qty = itemRequest.getQuantity() == null ? 0 : itemRequest.getQuantity();
            if (!isExchange && qty <= 0) {
                return ResponseEntity.badRequest().body("Quantity must be greater than 0.");
            }
            if (isExchange && qty == 0) {
                return ResponseEntity.badRequest().body("Quantity must not be 0.");
            }

            Product product = productRepository.findById(itemRequest.getProductId()).orElse(null);
            if (product == null) {
                return ResponseEntity.badRequest().body("Product not found: " + itemRequest.getProductId());
            }

            BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));
            total = total.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(qty);
            orderItem.setPrice(price);
            items.add(orderItem);
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);
        items.forEach(item -> item.setOrder(savedOrder));
        orderItemRepository.saveAll(items);

        String paymentToken = null;
        if (paid) {
            Payment payment = new Payment();
            payment.setOrder(savedOrder);
            payment.setMethod(request.getPaymentMethod() == null ? "CASH" : request.getPaymentMethod());
            payment.setAmount(total);
            payment.setStatus("PAID");
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        } else if ("TRANSFER".equalsIgnoreCase(request.getPaymentMethod())) {
            Payment payment = new Payment();
            payment.setOrder(savedOrder);
            payment.setMethod("TRANSFER");
            payment.setAmount(total);
            payment.setStatus("PENDING");
            paymentToken = UUID.randomUUID().toString();
            payment.setToken(paymentToken);
            paymentRepository.save(payment);
        }

        return ResponseEntity.ok(new CreateOrderResponse(
                savedOrder.getId(),
                total,
                items.size(),
                paid,
                paymentToken,
                savedOrder.getInvoiceNumber()
        ));
    }

    @PostMapping("/{orderId}/pay")
    @Transactional
    public ResponseEntity<?> payOrder(@PathVariable Long orderId,
                                      @RequestBody Map<String, String> body) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(body.getOrDefault("method", "CASH"));
        payment.setAmount(order.getTotalAmount());
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setStatus("PAID");
        orderRepository.save(order);

        if (order.getCustomer() != null) {
            pointService.addPoints(
                    order.getCustomer().getId(),
                    order.getTotalAmount(),
                    "ORDER_" + order.getId()
            );
        }

        return ResponseEntity.ok("Payment recorded & points added");
    }
}
