package com.bizflow.adminorderservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bizflow.adminorderservice.entity.OrderRecord;

@Repository
public interface OrderRecordRepository extends JpaRepository<OrderRecord, Long> {

    List<OrderRecord> findByStatusIgnoreCase(String status);
}
