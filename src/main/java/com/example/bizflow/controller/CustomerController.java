package com.example.bizflow.controller;

import com.example.bizflow.dto.OrderSummaryResponse;
import com.example.bizflow.entity.Customer;
import com.example.bizflow.repository.CustomerRepository;
import com.example.bizflow.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerRepository customerRepository;
    private final OrderService orderService;

    public CustomerController(CustomerRepository customerRepository,
                              OrderService orderService) {
        this.customerRepository = customerRepository;
        this.orderService = orderService;
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
    public ResponseEntity<?> getAllCustomers() {
        try {
            List<Customer> customers = customerRepository.findAll();
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching customers: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
    public ResponseEntity<?> getCustomerById(@PathVariable @NonNull Long id) {
        try {
            return customerRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching customer: " + e.getMessage());
        }
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
    public ResponseEntity<?> createCustomer(@RequestBody @NonNull Customer customer) {
        try {
            Customer saved = customerRepository.save(customer);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error creating customer: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/orders")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
    public ResponseEntity<List<OrderSummaryResponse>> getCustomerOrderHistory(@PathVariable @NonNull Long id) {
        return ResponseEntity.ok(orderService.getCustomerOrderHistory(id));
    }
}
