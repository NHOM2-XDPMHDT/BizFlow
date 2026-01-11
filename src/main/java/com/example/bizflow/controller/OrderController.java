package com.example.bizflow.controller;

import com.example.bizflow.dto.CreateOrderRequest;
import com.example.bizflow.dto.CreateOrderResponse;
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

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.example.bizflow.dto.CreateOrderRequest;
import com.example.bizflow.dto.CreateOrderResponse;
import com.example.bizflow.dto.OrderItemRequest;
import com.example.bizflow.entity.*;
import com.example.bizflow.repository.*;
import com.example.bizflow.service.OrderService;
import com.example.bizflow.service.PointService;


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

    public OrderController(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            OrderService orderService,
            PointService pointService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.pointService = pointService;
    }

    // ================== TẠO ĐƠN + THANH TOÁN ==================
    @PostMapping
    @Transactional
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body("Order items are required");
        }

        User user = request.getUserId() == null ? null :
                userRepository.findById(request.getUserId()).orElse(null);

        Customer customer = request.getCustomerId() == null ? null :
                customerRepository.findById(request.getCustomerId()).orElse(null);

        boolean paid = Boolean.TRUE.equals(request.getPaid());

        Order order = new Order();
        order.setUser(user);
        order.setCustomer(customer);
        order.setStatus(paid ? "PAID" : "UNPAID");
        order.setInvoiceNumber(
                orderService.generateInvoiceNumberForDate(LocalDate.now(), "SALE")
        );

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (OrderItemRequest req : request.getItems()) {

            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            int qty = req.getQuantity();
            if (qty <= 0) {
                return ResponseEntity.badRequest().body("Quantity must be > 0");
            }

            BigDecimal lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(qty));

            total = total.add(lineTotal);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(qty);
            item.setPrice(product.getPrice());
            items.add(item);
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        items.forEach(i -> i.setOrder(savedOrder));
        orderItemRepository.saveAll(items);

        if (paid) {
            Payment payment = new Payment();
            payment.setOrder(savedOrder);
            payment.setMethod(
                    request.getPaymentMethod() == null ? "CASH" : request.getPaymentMethod()
            );
            payment.setAmount(total);
            payment.setStatus("PAID");
            payment.setPaidAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);

            // ✅ CỘNG ĐIỂM KHÁCH HÀNG (KHÔNG PHỤ THUỘC PHƯƠNG THỨC)
            if (customer != null) {
                pointService.addPoints(
                        customer.getId(),
                        total,
                        "ORDER_" + savedOrder.getId()
                );
            }
        }

        return ResponseEntity.ok(
                new CreateOrderResponse(
                        savedOrder.getId(),
                        total,
                        items.size(),
                        paid,
                        null
                )
        );
    }
}
