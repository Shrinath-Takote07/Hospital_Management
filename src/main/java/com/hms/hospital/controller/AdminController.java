
package com.hms.hospital.controller;

import com.hms.hospital.dto.TransactionVO;
import com.hms.hospital.entity.*;
import com.hms.hospital.repository.*;
import com.hms.hospital.service.PdfGeneratorService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // Only Admin can access
public class AdminController {

    private final UserRepository userRepo;
    private final PatientRepository patientRepo;
    private final AppointmentRepository appointmentRepo;
    private final BedAllocationRepository bedAllocationRepo;
    private final HospitalBillRepository hospitalBillRepo;
    private final PdfGeneratorService pdfGeneratorService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userRepo.count());
        model.addAttribute("totalPatients", patientRepo.count());
        model.addAttribute("totalAppointments", appointmentRepo.count());
        model.addAttribute("totalDoctors", userRepo.countByRole(Role.DOCTOR));
        model.addAttribute("totalBeds", bedAllocationRepo.count());
        model.addAttribute("recentAppointments", appointmentRepo.findTop10ByOrderByStartTimeDesc());
        return "admin/dashboard";
    }

    @GetMapping("/beds")
    public String bedManagement(Model model) {
        model.addAttribute("beds", bedAllocationRepo.findAllByOrderByIdDesc());
        model.addAttribute("patients", patientRepo.findAll());
        return "admin/beds";
    }

    @PostMapping("/beds/create")
    public String createBed(@RequestParam String bedType,
                            @RequestParam String roomNo,
                            @RequestParam String bedNo,
                            @RequestParam Double dailyFee,
                            RedirectAttributes ra) {
        BedAllocation bed = new BedAllocation();
        bed.setBedType(bedType);
        bed.setRoomNo(roomNo);
        bed.setBedNo(bedNo);
        bed.setDailyFee(dailyFee);
        bed.setStatus("AVAILABLE");
        bed.setPaymentStatus("PENDING");
        bedAllocationRepo.save(bed);
        ra.addFlashAttribute("msg", "Bed " + bedNo + " (" + bedType + ") added successfully!");
        return "redirect:/admin/beds";
    }

    @PostMapping("/beds/{id}/allocate")
    public String allocateBed(@PathVariable Long id,
                              @RequestParam Long patientId,
                              RedirectAttributes ra) {
        BedAllocation bed = bedAllocationRepo.findById(id).orElse(null);
        Patient patient = patientRepo.findById(patientId).orElse(null);
        if (bed != null && patient != null) {
            bed.setPatient(patient);
            bed.setStatus("OCCUPIED");
            bed.setAllocatedDate(LocalDateTime.now());
            bed.setDischargeDate(null);
            bedAllocationRepo.save(bed);
            ra.addFlashAttribute("msg", "Bed " + bed.getBedNo() + " allocated to patient " + patient.getName() + "!");
        }
        return "redirect:/admin/beds";
    }

    @PostMapping("/beds/{id}/release")
    public String releaseBed(@PathVariable Long id, RedirectAttributes ra) {
        BedAllocation bed = bedAllocationRepo.findById(id).orElse(null);
        if (bed != null) {
            bed.setDischargeDate(LocalDateTime.now());
            bed.setStatus("AVAILABLE");
            bed.setPatient(null);
            bedAllocationRepo.save(bed);
            ra.addFlashAttribute("msg", "Bed " + bed.getBedNo() + " released & marked as Available!");
        }
        return "redirect:/admin/beds";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userRepo.findAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/delete")
    @Transactional
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        User user = userRepo.findById(id).orElse(null);
        if (user != null) {
            if (user.getPatient() != null) {
                List<Appointment> pApts = appointmentRepo.findByPatientId(user.getPatient().getId());
                appointmentRepo.deleteAll(pApts);
            }
            List<Appointment> docApts = appointmentRepo.findByDoctorId(user.getId());
            appointmentRepo.deleteAll(docApts);

            userRepo.delete(user);
            ra.addFlashAttribute("msg", "User deleted successfully!");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role, RedirectAttributes ra) {
        User user = userRepo.findById(id).orElseThrow();
        user.setRole(role);
        userRepo.save(user);
        ra.addFlashAttribute("msg", "Role changed to " + role + " successfully!");
        return "redirect:/admin/users";
    }

    @PostMapping("/doctors/{id}/update-status-fee")
    public String updateDoctorStatusAndFee(@PathVariable Long id,
                                          @RequestParam(required = false) Double fees,
                                          @RequestParam(required = false, defaultValue = "AVAILABLE") String availableStatus,
                                          RedirectAttributes ra) {
        User doctor = userRepo.findById(id).orElse(null);
        if (doctor != null && doctor.getRole() == Role.DOCTOR) {
            if (fees != null) {
                doctor.setFees(fees);
            }
            if (availableStatus != null) {
                doctor.setAvailableStatus(availableStatus);
            }
            userRepo.save(doctor);
            ra.addFlashAttribute("msg", "Doctor details updated: Fee ₹" + doctor.getFees() + " & Status: " + doctor.getAvailableStatus());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/appointments")
    public String allAppointments(Model model) {
        model.addAttribute("appointments", appointmentRepo.findAllByOrderByStartTimeDesc());
        return "admin/appointments";
    }

    @RequestMapping(value = "/appointments/{id}/confirm-payment", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public String confirmPayment(@PathVariable Long id,
                                 @RequestParam(required = false, defaultValue = "ONLINE") String paymentCategory,
                                 @RequestParam(required = false, defaultValue = "UPI QR") String paymentMode,
                                 @RequestParam(required = false) String paymentId,
                                 RedirectAttributes ra) {
        Appointment appointment = appointmentRepo.findById(id).orElse(null);
        if (appointment != null) {
            appointment.setPaymentStatus("PAID");
            String fullPayMode = paymentCategory + " (" + paymentMode + ")";
            appointment.setPaymentMode(fullPayMode);
            appointment.setPaymentDate(LocalDateTime.now());
            if (paymentId == null || paymentId.trim().isEmpty()) {
                appointment.setPaymentId(paymentMode.toUpperCase().replace(" ", "-") + "-" + (System.currentTimeMillis() % 1000000));
            } else {
                appointment.setPaymentId(paymentId);
            }
            if (appointment.getAmountPaid() == null && appointment.getDoctor() != null) {
                appointment.setAmountPaid(appointment.getDoctor().getFees() != null ? appointment.getDoctor().getFees() : 500.0);
            }
            appointmentRepo.save(appointment);
            ra.addFlashAttribute("msg", "Payment confirmed via " + fullPayMode + " for Appointment #" + id + "! PDF receipt unlocked.");
        }
        return "redirect:/admin/appointments";
    }

    @GetMapping("/appointments/{id}/receipt")
    public ResponseEntity<?> downloadReceipt(@PathVariable Long id, RedirectAttributes ra) {
        Appointment appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!"PAID".equalsIgnoreCase(appointment.getPaymentStatus())) {
            return ResponseEntity.status(403).body("Receipt download unavailable. Payment has not been confirmed by Admin yet.");
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

    // --- Hospital Billing Management (Offline & Online Patient Services) ---
    @GetMapping("/bills")
    public String viewHospitalBills(Model model) {
        model.addAttribute("bills", hospitalBillRepo.findAllByOrderByBillDateDesc());
        model.addAttribute("patients", patientRepo.findAll());
        model.addAttribute("doctors", userRepo.findAll().stream().filter(u -> u.getRole() == Role.DOCTOR).toList());
        return "admin/bills";
    }

    @PostMapping("/bills/create")
    public String createHospitalBill(@RequestParam String patientName,
                                     @RequestParam(required = false) String patientPhone,
                                     @RequestParam(required = false) String doctorName,
                                     @RequestParam(required = false, defaultValue = "OFFLINE") String paymentCategory,
                                     @RequestParam(required = false, defaultValue = "Cash") String paymentMode,
                                     @RequestParam(required = false) String transactionId,
                                     @RequestParam(required = false) String[] serviceName,
                                     @RequestParam(required = false) Double[] serviceFee,
                                     @RequestParam(required = false) String notes,
                                     RedirectAttributes ra) {

        double totalAmount = 0.0;
        StringBuilder servicesSummary = new StringBuilder();

        if (serviceName != null && serviceFee != null) {
            for (int i = 0; i < Math.min(serviceName.length, serviceFee.length); i++) {
                if (serviceName[i] != null && !serviceName[i].trim().isEmpty() && serviceFee[i] != null) {
                    if (servicesSummary.length() > 0) servicesSummary.append(", ");
                    servicesSummary.append(serviceName[i].trim()).append(" (₹").append(serviceFee[i]).append(")");
                    totalAmount += serviceFee[i];
                }
            }
        }

        if (totalAmount <= 0) {
            totalAmount = 500.0;
            if (servicesSummary.length() == 0) {
                servicesSummary.append("Doctor Consultation & OPD Services (₹500.0)");
            }
        }

        String billNo = "BILL-" + System.currentTimeMillis() % 10000000;

        HospitalBill bill = HospitalBill.builder()
                .billNumber(billNo)
                .patientName(patientName)
                .patientPhone(patientPhone)
                .doctorName(doctorName != null && !doctorName.isEmpty() ? doctorName : "General Duty Doctor")
                .billDate(LocalDateTime.now())
                .paymentCategory(paymentCategory)
                .paymentMode(paymentMode)
                .transactionId(transactionId != null && !transactionId.isEmpty() ? transactionId : (paymentMode.equalsIgnoreCase("Cash") ? "CASH-OFFLINE" : "TXN-" + System.currentTimeMillis() % 1000000))
                .paymentStatus("PAID")
                .servicesSummary(servicesSummary.toString())
                .totalAmount(totalAmount)
                .notes(notes)
                .build();

        hospitalBillRepo.save(bill);

        ra.addFlashAttribute("msg", "Hospital Bill #" + billNo + " created successfully! Total: ₹" + totalAmount);
        return "redirect:/admin/bills";
    }

    @GetMapping("/bills/{id}/pdf")
    public ResponseEntity<?> downloadHospitalBillPdf(@PathVariable Long id) {
        HospitalBill bill = hospitalBillRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Hospital Bill not found"));

        ByteArrayInputStream bis = pdfGeneratorService.generateHospitalBillPdf(bill);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=hospital-bill-" + bill.getBillNumber() + ".pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }
    
    @PostMapping("/appointments/{id}/complete")
    public String completeAppointment(@PathVariable Long id, RedirectAttributes ra) {
        Appointment appointment = appointmentRepo.findById(id).orElse(null);
        if (appointment != null) {
            appointment.setStatus("COMPLETED");
            appointmentRepo.save(appointment);
            ra.addFlashAttribute("msg", "Appointment marked as completed!");
        }
        return "redirect:/admin/appointments";
    }

    @PostMapping("/appointments/{id}/delete")
    public String deleteAppointment(@PathVariable Long id,
                                    RedirectAttributes ra) {

        appointmentRepo.deleteById(id);
        ra.addFlashAttribute("msg", "Appointment deleted successfully!");
        return "redirect:/admin/appointments";
    }

    // --- Secure Transaction Analytics & Revenue Tracking ---
    @GetMapping("/transactions")
    public String transactionAnalytics(HttpSession session, Model model) {
        Boolean authorized = (Boolean) session.getAttribute("analyticsAuthorized");
        if (authorized == null || !authorized) {
            return "admin/transactions-lock";
        }

        List<TransactionVO> txList = new ArrayList<>();
        double totalRevenue = 0.0;
        double onlineRevenue = 0.0;
        double offlineRevenue = 0.0;

        double cashAmount = 0.0;
        double upiAmount = 0.0;
        double cardAmount = 0.0;
        double netBankingAmount = 0.0;

        // 1. Appointments (Paid)
        List<Appointment> paidApts = appointmentRepo.findAll().stream()
                .filter(a -> "PAID".equalsIgnoreCase(a.getPaymentStatus()))
                .toList();

        for (Appointment apt : paidApts) {
            double amt = apt.getAmountPaid() != null ? apt.getAmountPaid() : (apt.getDoctor() != null && apt.getDoctor().getFees() != null ? apt.getDoctor().getFees() : 500.0);
            totalRevenue += amt;

            String mode = apt.getPaymentMode() != null ? apt.getPaymentMode() : "UPI QR";
            String cat = mode.toUpperCase().contains("OFFLINE") || mode.toUpperCase().contains("CASH") ? "OFFLINE" : "ONLINE";

            if ("OFFLINE".equalsIgnoreCase(cat)) {
                offlineRevenue += amt;
                cashAmount += amt;
            } else {
                onlineRevenue += amt;
                if (mode.toLowerCase().contains("card")) cardAmount += amt;
                else if (mode.toLowerCase().contains("net")) netBankingAmount += amt;
                else upiAmount += amt;
            }

            txList.add(TransactionVO.builder()
                    .id("APT-" + apt.getId())
                    .type("APPOINTMENT")
                    .referenceNo(apt.getPaymentId() != null ? apt.getPaymentId() : "TXN-APT-" + apt.getId())
                    .patientName(apt.getPatient() != null ? apt.getPatient().getName() : "Patient #" + apt.getId())
                    .serviceSummary("Doctor Appointment: Dr. " + (apt.getDoctor() != null ? apt.getDoctor().getName() : "Consultant"))
                    .amount(amt)
                    .paymentCategory(cat)
                    .paymentMode(mode)
                    .paymentStatus("PAID")
                    .date(apt.getPaymentDate() != null ? apt.getPaymentDate() : apt.getStartTime())
                    .receiptUrl("/admin/appointments/" + apt.getId() + "/receipt")
                    .build());
        }

        // 2. Hospital Bills (Paid)
        List<HospitalBill> bills = hospitalBillRepo.findAll();
        for (HospitalBill bill : bills) {
            double amt = bill.getTotalAmount() != null ? bill.getTotalAmount() : 0.0;
            totalRevenue += amt;

            String cat = bill.getPaymentCategory() != null ? bill.getPaymentCategory() : "OFFLINE";
            String mode = bill.getPaymentMode() != null ? bill.getPaymentMode() : "Cash";

            if ("OFFLINE".equalsIgnoreCase(cat) || mode.equalsIgnoreCase("Cash")) {
                offlineRevenue += amt;
                cashAmount += amt;
            } else {
                onlineRevenue += amt;
                if (mode.toLowerCase().contains("card")) cardAmount += amt;
                else if (mode.toLowerCase().contains("net")) netBankingAmount += amt;
                else upiAmount += amt;
            }

            txList.add(TransactionVO.builder()
                    .id(bill.getBillNumber())
                    .type("HOSPITAL_BILL")
                    .referenceNo(bill.getTransactionId() != null ? bill.getTransactionId() : bill.getBillNumber())
                    .patientName(bill.getPatientName() != null ? bill.getPatientName() : "Walk-in Patient")
                    .serviceSummary(bill.getServicesSummary() != null ? bill.getServicesSummary() : "Hospital Care & Diagnostic Services")
                    .amount(amt)
                    .paymentCategory(cat)
                    .paymentMode(mode)
                    .paymentStatus(bill.getPaymentStatus() != null ? bill.getPaymentStatus() : "PAID")
                    .date(bill.getBillDate() != null ? bill.getBillDate() : LocalDateTime.now())
                    .receiptUrl("/admin/bills/" + bill.getId() + "/pdf")
                    .build());
        }

        // Sort descending by date
        txList.sort((a, b) -> {
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        model.addAttribute("transactions", txList);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalCount", txList.size());
        model.addAttribute("onlineRevenue", onlineRevenue);
        model.addAttribute("offlineRevenue", offlineRevenue);
        model.addAttribute("cashAmount", cashAmount);
        model.addAttribute("upiAmount", upiAmount);
        model.addAttribute("cardAmount", cardAmount);
        model.addAttribute("netBankingAmount", netBankingAmount);

        return "admin/transactions";
    }

    @PostMapping("/transactions/auth")
    public String authTransactions(@RequestParam String password, HttpSession session, RedirectAttributes ra) {
        if ("shrinath".equals(password)) {
            session.setAttribute("analyticsAuthorized", true);
            ra.addFlashAttribute("msg", "🔒 Analytics & Transaction Access Unlocked!");
            return "redirect:/admin/transactions";
        } else {
            ra.addFlashAttribute("error", "❌ Incorrect Security Password! Access Denied.");
            return "redirect:/admin/transactions";
        }
    }

    @PostMapping("/transactions/lock")
    public String lockTransactions(HttpSession session, RedirectAttributes ra) {
        session.removeAttribute("analyticsAuthorized");
        ra.addFlashAttribute("msg", "🔒 Transaction Analytics Locked.");
        return "redirect:/admin/dashboard";
    }
}