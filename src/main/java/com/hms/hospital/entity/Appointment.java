package com.hms.hospital.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Builder.Default
    private String status = "SCHEDULED";

    @Builder.Default
    private String appointmentType = "REGULAR"; // REGULAR, URGENT, FOLLOW_UP

    @Column(length = 1000)
    private String patientMessage;

    @Builder.Default
    private String paymentStatus = "PENDING"; // PENDING, PAID, REFUNDED

    private String paymentMode; // UPI QR, CASH, CARD

    private String paymentId;

    private LocalDateTime paymentDate;

    private Double amountPaid;

    @ManyToOne
    private Patient patient;

    @ManyToOne
    private User doctor;
}