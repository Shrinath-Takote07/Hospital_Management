package com.hms.hospital.repository;

import com.hms.hospital.entity.HospitalBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalBillRepository extends JpaRepository<HospitalBill, Long> {
    List<HospitalBill> findAllByOrderByBillDateDesc();
}
