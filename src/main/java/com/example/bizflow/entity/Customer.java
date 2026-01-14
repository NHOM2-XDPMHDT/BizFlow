package com.example.bizflow.entity;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== THÔNG TIN CƠ BẢN =====
    @Column(nullable = false)
    private String name;

    private String phone;
    private String email;
    private String address;

    // ===== TÍCH ĐIỂM =====
    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(name = "monthly_points", nullable = false)
    private Integer monthlyPoints = 0;

    @Enumerated(EnumType.STRING)
    private CustomerTier tier = CustomerTier.DONG;

    // ===== THÔNG TIN MỞ RỘNG =====
    private LocalDate dob;
    private String cccd;

    public Customer(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    // ===== HELPER METHOD =====
    public void addPoints(int points) {
        if (points <= 0) return;

        if (totalPoints == null) totalPoints = 0;
        if (monthlyPoints == null) monthlyPoints = 0;

        totalPoints += points;
        monthlyPoints += points;
    }

    public Long getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Integer getTotalPoints() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
