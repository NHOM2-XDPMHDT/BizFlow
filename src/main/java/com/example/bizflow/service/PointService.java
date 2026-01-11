package com.example.bizflow.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bizflow.entity.Customer;
import com.example.bizflow.entity.CustomerTier;
import com.example.bizflow.entity.PointHistory;
import com.example.bizflow.repository.CustomerRepository;
import com.example.bizflow.repository.PointHistoryRepository;

@Service
public class PointService {

    private final CustomerRepository customerRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // ✅ CONSTRUCTOR INJECTION (CHẮC CHẮN CHẠY)
    public PointService(CustomerRepository customerRepository,
                        PointHistoryRepository pointHistoryRepository) {
        this.customerRepository = customerRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    @Transactional
    public void addPoints(Long customerId,
                          BigDecimal totalAmount,
                          String reference) {

        // 1️⃣ chống cộng trùng
        if (pointHistoryRepository.existsByReference(reference)) {
            return;
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // 2️⃣ 1000đ = 1 điểm
        int points = totalAmount
                .divide(BigDecimal.valueOf(1000))
                .intValue();

        if (points <= 0) return;

        // 3️⃣ lưu lịch sử điểm
        PointHistory history = new PointHistory();
        history.setCustomer(customer);
        history.setPoints(points);
        history.setType("EARN");
        history.setReference(reference);
        pointHistoryRepository.save(history);

        // 4️⃣ cập nhật customer
        customer.addPoints(points);

        // 5️⃣ update tier
        customer.setTier(calculateTier(customer.getTotalPoints()));

        customerRepository.save(customer);
    }

    private CustomerTier calculateTier(int totalPoints) {
        if (totalPoints >= 50000) return CustomerTier.KIM_CUONG;
        if (totalPoints >= 30000) return CustomerTier.BACH_KIM;
        if (totalPoints >= 15000) return CustomerTier.VANG;
        if (totalPoints >= 5000)  return CustomerTier.BAC;
        return CustomerTier.DONG;
    }
}
