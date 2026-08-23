package com.hms.hospital.repository;

import com.hms.hospital.entity.BedAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BedAllocationRepository extends JpaRepository<BedAllocation, Long> {
    List<BedAllocation> findByStatus(String status);
    List<BedAllocation> findByPatientId(Long patientId);
    List<BedAllocation> findAllByOrderByIdDesc();
}
