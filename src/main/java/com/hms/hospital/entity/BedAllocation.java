package com.hms.hospital.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Table(name = "bed_allocations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BedAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bedType;     // ICU, GENERAL, EMERGENCY, PRIVATE_WARD, PEDIATRIC_ICU
    private String roomNo;      // e.g. Room-102
    private String bedNo;       // e.g. BED-05
    private Double dailyFee;    // Daily fee in INR (₹)
    private String status;      // AVAILABLE, OCCUPIED, MAINTENANCE

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime allocatedDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dischargeDate;

    private String paymentStatus; // PENDING, PAID
}
