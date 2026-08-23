package com.hms.hospital.controller;
import com.hms.hospital.entity.Patient;
import com.hms.hospital.entity.Role;
import com.hms.hospital.entity.User;
import com.hms.hospital.repository.AppointmentRepository;
import com.hms.hospital.repository.PatientRepository;
import com.hms.hospital.repository.UserRepository;
import com.hms.hospital.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import java.security.Principal;
import java.time.LocalDateTime;

import com.hms.hospital.service.PdfGeneratorService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.ByteArrayInputStream;

@Controller
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final PatientRepository patientRepo;
    private final UserRepository userRepo;
    private final AppointmentRepository appointmentRepo;
    private final com.hms.hospital.repository.HospitalBillRepository hospitalBillRepo;
    private final PasswordEncoder passwordEncoder;
    private final PdfGeneratorService pdfGeneratorService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("patient", new Patient());
        return "register";
    }

    @PostMapping("/register")
    public String registerPatient(@Valid @ModelAttribute("patient") Patient patient,
                                  BindingResult result,
                                  @RequestParam String password,
                                  @RequestParam(value = "govtIdFile", required = false) org.springframework.web.multipart.MultipartFile govtIdFile,
                                  @RequestParam(value = "photoFile", required = false) org.springframework.web.multipart.MultipartFile photoFile,
                                  RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "register";
        }

        if (userRepo.findByEmail(patient.getEmail()).isPresent()) {
            ra.addFlashAttribute("error", "Email already registered!");
            return "redirect:/register";
        }

        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            java.io.File dir = new java.io.File(uploadDir + "ids/");
            if (!dir.exists()) dir.mkdirs();
            java.io.File photoDir = new java.io.File(uploadDir + "photos/");
            if (!photoDir.exists()) photoDir.mkdirs();

            if (govtIdFile != null && !govtIdFile.isEmpty()) {
                String govtIdFileName = System.currentTimeMillis() + "_" + govtIdFile.getOriginalFilename();
                java.nio.file.Path path = java.nio.file.Paths.get(uploadDir + "ids/" + govtIdFileName);
                java.nio.file.Files.write(path, govtIdFile.getBytes());
                patient.setGovtIdProofPath("/uploads/ids/" + govtIdFileName);
            }

            if (photoFile != null && !photoFile.isEmpty()) {
                String photoFileName = System.currentTimeMillis() + "_" + photoFile.getOriginalFilename();
                java.nio.file.Path photoPath = java.nio.file.Paths.get(uploadDir + "photos/" + photoFileName);
                java.nio.file.Files.write(photoPath, photoFile.getBytes());
                patient.setPhotoPath("/uploads/photos/" + photoFileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        User user = new User();
        user.setName(patient.getName());
        user.setEmail(patient.getEmail());
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(patient.getPhone());
        user.setRole(Role.PATIENT);
        userRepo.save(user);

        patient.setPatientId("PAT-" + String.format("%04d", patientRepo.count() + 1));
        patient.setUser(user);
        patientRepo.save(patient);

        ra.addFlashAttribute("msg", "Registration completed successfully with Govt ID & Photo proof!");
        return "redirect:/login";
    }

    @GetMapping("/patient/dashboard")
    public String patientDashboard(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        Patient patient = patientService.findPatientByEmail(principal.getName()).orElse(null);
        if (patient == null) {
            return "redirect:/dashboard";
        }
        long upcomingCount = appointmentRepo.countByPatientIdAndStartTimeAfter(patient.getId(), LocalDateTime.now());
        long completedCount = appointmentRepo.countByPatientIdAndStartTimeBefore(patient.getId(), LocalDateTime.now());

        var myAppointments = appointmentRepo.findByPatientId(patient.getId());
        var doctors = userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.DOCTOR)
                .toList();

        // Fetch hospital bills matching patient name or email or phone
        var myHospitalBills = hospitalBillRepo.findAll().stream()
                .filter(b -> (b.getPatientName() != null && b.getPatientName().equalsIgnoreCase(patient.getName()))
                        || (b.getPatientEmail() != null && b.getPatientEmail().equalsIgnoreCase(patient.getEmail()))
                        || (patient.getPhone() != null && b.getPatientPhone() != null && b.getPatientPhone().contains(patient.getPhone())))
                .toList();

        model.addAttribute("patient", patient);
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("myAppointments", myAppointments);
        model.addAttribute("myHospitalBills", myHospitalBills);
        model.addAttribute("doctors", doctors);
        return "patient/dashboard";
    }

    @GetMapping("/patient/appointments/{id}/receipt")
    public ResponseEntity<?> downloadReceipt(@PathVariable Long id, Principal principal) {
        var appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!"PAID".equalsIgnoreCase(appointment.getPaymentStatus())) {
            return ResponseEntity.status(403).body("Receipt unavailable: Admin has not confirmed payment for this appointment yet.");
        }

        ByteArrayInputStream bis = pdfGeneratorService.generateAppointmentReceipt(appointment);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=receipt-appointment-" + id + ".pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    @GetMapping("/patient/bills/{id}/pdf")
    public ResponseEntity<?> downloadHospitalBillPdf(@PathVariable Long id, Principal principal) {
        var bill = hospitalBillRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Hospital Bill not found"));

        if (!"PAID".equalsIgnoreCase(bill.getPaymentStatus())) {
            return ResponseEntity.status(403).body("Receipt download locked. Payment must be confirmed by Admin first.");
        }

        ByteArrayInputStream bis = pdfGeneratorService.generateHospitalBillPdf(bill);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=hospital-bill-" + bill.getBillNumber() + ".pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }
}