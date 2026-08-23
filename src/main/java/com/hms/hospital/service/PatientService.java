package com.hms.hospital.service;

import com.hms.hospital.entity.*;
import com.hms.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public void registerPatient(Patient patient, String rawPassword) {
        if (patientRepo.existsByEmail(patient.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        User user = new User();
        user.setName(patient.getName());
        user.setEmail(patient.getEmail());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.PATIENT);
        userRepo.save(user);

        patient.setPatientId("PAT-" + String.format("%04d", patientRepo.count() + 1));
        patient.setUser(user);
        patientRepo.save(patient);
    }


    public Optional<Patient> findPatientByEmail(String email) {
        return patientRepo.findByUserEmail(email);
    }

    public Patient getPatientByEmail(String email) {
        return findPatientByEmail(email)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }
}