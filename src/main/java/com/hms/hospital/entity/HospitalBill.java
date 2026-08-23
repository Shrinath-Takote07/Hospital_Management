package com.hms.hospital.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hospital_bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String billNumber; // e.g. BILL-2026-001

    private String patientName;
    private String patientPhone;
    private String patientEmail;

    private String doctorName;

    private LocalDateTime billDate;

    private String paymentCategory; // ONLINE, OFFLINE
    private String paymentMode;     // Cash, Credit Card, Debit Card, UPI QR, Net Banking
    private String transactionId;

    private String paymentStatus;   // PAID, PENDING

    @Column(length = 2000)
    private String servicesSummary; // Comma separated or description of services (e.g. X-Ray: ₹750, CT Scan: ₹2500)

    @Column(length = 4000)
    private String itemizedDetailsJson; // JSON array or formatted line items

    private Double totalAmount;

    private String notes;
}
