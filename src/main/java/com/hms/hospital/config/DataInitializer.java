package com.hms.hospital.config;

import com.hms.hospital.entity.Patient;
import com.hms.hospital.entity.Role;
import com.hms.hospital.entity.User;
import com.hms.hospital.repository.PatientRepository;
import com.hms.hospital.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Create Fake Admin if not exists or update password and role if needed
        userRepository.findByEmail("admin@hospital.com").ifPresentOrElse(admin -> {
            boolean updated = false;
            if (!admin.getPassword().startsWith("$2a$") && !admin.getPassword().startsWith("$2b$")) {
                admin.setPassword(passwordEncoder.encode("admin123"));
                updated = true;
            }
            if (admin.getRole() != Role.ADMIN) {
                admin.setRole(Role.ADMIN);
                updated = true;
            }
            if (updated) {
                userRepository.save(admin);
                System.out.println("Updated Admin user credentials/role.");
            }
        }, () -> {
            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail("admin@hospital.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("Created default Admin user: admin@hospital.com / admin123");
        });

        // 2. Create Fake Doctor 1 if not exists or update password and role if needed
        userRepository.findByEmail("doctor@hospital.com").ifPresentOrElse(doctor -> {
            boolean updated = false;
            if (!doctor.getPassword().startsWith("$2a$") && !doctor.getPassword().startsWith("$2b$")) {
                doctor.setPassword(passwordEncoder.encode("doctor123"));
                updated = true;
            }
            if (doctor.getRole() != Role.DOCTOR) {
                doctor.setRole(Role.DOCTOR);
                updated = true;
            }
            if (doctor.getSpecialization() == null) {
                doctor.setSpecialization("Cardiology");
                doctor.setFees(120.0);
                doctor.setBio("Senior Cardiologist with 12+ years of experience in cardiovascular health, heart care, and preventive medicine.");
                doctor.setPhone("+1 (555) 234-5678");
                updated = true;
            }
            if (updated) {
                userRepository.save(doctor);
                System.out.println("Updated Doctor user credentials/role/bio.");
            }
        }, () -> {
            User doctor = new User();
            doctor.setName("Dr. Sarah Jenkins");
            doctor.setEmail("doctor@hospital.com");
            doctor.setPassword(passwordEncoder.encode("doctor123"));
            doctor.setRole(Role.DOCTOR);
            doctor.setSpecialization("Cardiology");
            doctor.setFees(120.0);
            doctor.setBio("Senior Cardiologist with 12+ years of experience in cardiovascular health, heart care, and preventive medicine.");
            doctor.setPhone("+1 (555) 234-5678");
            userRepository.save(doctor);
            System.out.println("Created default Doctor user: doctor@hospital.com / doctor123");
        });

        // 3. Create Fake Doctor 2 if not exists
        userRepository.findByEmail("doctor2@hospital.com").ifPresentOrElse(doctor2 -> {
            boolean updated = false;
            if (!doctor2.getPassword().startsWith("$2a$") && !doctor2.getPassword().startsWith("$2b$")) {
                doctor2.setPassword(passwordEncoder.encode("doctor123"));
                updated = true;
            }
            if (doctor2.getRole() != Role.DOCTOR) {
                doctor2.setRole(Role.DOCTOR);
                updated = true;
            }
            if (doctor2.getSpecialization() == null) {
                doctor2.setSpecialization("General Physician");
                doctor2.setFees(75.0);
                doctor2.setBio("Experienced General Physician specializing in internal medicine, routine checkups, and chronic disease management.");
                doctor2.setPhone("+1 (555) 876-5432");
                updated = true;
            }
            if (updated) {
                userRepository.save(doctor2);
            }
        }, () -> {
            User doctor2 = new User();
            doctor2.setName("Dr. John Watson");
            doctor2.setEmail("doctor2@hospital.com");
            doctor2.setPassword(passwordEncoder.encode("doctor123"));
            doctor2.setRole(Role.DOCTOR);
            doctor2.setSpecialization("General Physician");
            doctor2.setFees(75.0);
            doctor2.setBio("Experienced General Physician specializing in internal medicine, routine checkups, and chronic disease management.");
            doctor2.setPhone("+1 (555) 876-5432");
            userRepository.save(doctor2);
            System.out.println("Created default Doctor 2 user: doctor2@hospital.com / doctor123");
        });

        // 4. Create Fake Patient if not exists or update password and role if needed
        userRepository.findByEmail("patient@hospital.com").ifPresentOrElse(patientUser -> {
            boolean updated = false;
            if (!patientUser.getPassword().startsWith("$2a$") && !patientUser.getPassword().startsWith("$2b$")) {
                patientUser.setPassword(passwordEncoder.encode("patient123"));
                updated = true;
            }
            if (patientUser.getRole() != Role.PATIENT) {
                patientUser.setRole(Role.PATIENT);
                updated = true;
            }
            if (updated) {
                userRepository.save(patientUser);
                System.out.println("Updated Patient user credentials/role.");
            }
        }, () -> {
            User patientUser = new User();
            patientUser.setName("Alex Johnson");
            patientUser.setEmail("patient@hospital.com");
            patientUser.setPassword(passwordEncoder.encode("patient123"));
            patientUser.setRole(Role.PATIENT);
            userRepository.save(patientUser);

            Patient patient = new Patient();
            patient.setName("Alex Johnson");
            patient.setEmail("patient@hospital.com");
            patient.setPhone("9876543210");
            patient.setGender("Male");
            patient.setDob(LocalDate.of(1995, 5, 15));
            patient.setPatientId("PAT-0001");
            patient.setUser(patientUser);
            patientRepository.save(patient);
            System.out.println("Created default Patient user: patient@hospital.com / patient123");
        });
    }
}
