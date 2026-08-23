package com.hms.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionVO {
    private String id;
    private String type; // "APPOINTMENT" or "HOSPITAL_BILL"
    private String referenceNo;
    private String patientName;
    private String serviceSummary;
    private Double amount;
    private String paymentCategory; // ONLINE, OFFLINE
    private String paymentMode;     // UPI QR, Cash, Credit Card, Debit Card, Net Banking
    private String paymentStatus;   // PAID, PENDING
    private LocalDateTime date;
    private String receiptUrl;
}
