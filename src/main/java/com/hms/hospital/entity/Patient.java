package com.hms.hospital.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "patients")
@Data @NoArgsConstructor @AllArgsConstructor
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientId;
    private String name;
    private String email;
    private String phone;
    private String gender;

    @Column(name = "date_of_birth")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    private String govtIdType;
    private String govtIdNumber;
    private String govtIdProofPath;
    private String photoPath;

    @Column(length = 500)
    private String permanentAddress;

    @OneToOne
    private User user;
}