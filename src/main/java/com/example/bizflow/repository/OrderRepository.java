package com.example.bizflow.repository;

import com.example.bizflow.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.user LEFT JOIN FETCH o.customer ORDER BY o.createdAt DESC")
    List<Order> findAllWithDetails();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.user LEFT JOIN FETCH o.customer ORDER BY o.createdAt DESC")
    List<Order> findAllByOrderByCreatedAtDesc();

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.user LEFT JOIN FETCH o.customer WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"items", "items.product", "user", "customer"})
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Order> findTopByInvoiceNumberStartingWithOrderByInvoiceNumberDesc(String prefix);

    @EntityGraph(attributePaths = {"items", "items.product", "user", "customer"})
    Optional<Order> findByInvoiceNumber(String invoiceNumber);

    @Query("""
        SELECT o FROM Order o
        LEFT JOIN o.customer c
        WHERE (:keyword IS NULL OR o.invoiceNumber LIKE %:keyword% OR c.phone LIKE %:keyword%)
          AND (:fromDate IS NULL OR o.createdAt >= :fromDate)
          AND (:toDate IS NULL OR o.createdAt <= :toDate)
        ORDER BY o.createdAt DESC
    """)
    List<Order> searchOrders(@Param("keyword") String keyword,
                             @Param("fromDate") java.time.LocalDateTime fromDate,
                             @Param("toDate") java.time.LocalDateTime toDate);
}
