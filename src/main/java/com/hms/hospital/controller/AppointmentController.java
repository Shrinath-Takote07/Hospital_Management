package com.hms.hospital.controller;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hms.hospital.entity.Appointment;
import com.hms.hospital.entity.Patient;
import com.hms.hospital.entity.Role;
import com.hms.hospital.entity.User;
import com.hms.hospital.repository.AppointmentRepository;
import com.hms.hospital.repository.UserRepository;
import com.hms.hospital.service.PatientService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/appointment")
public class AppointmentController {

    private final AppointmentRepository appointmentRepo;
    private final PatientService patientService;
    private final UserRepository userRepo;

    @GetMapping("/calendar")
    public String calendar(Model model, Principal principal) {
        model.addAttribute("userEmail", principal.getName());
        return "appointment/calendar";
    }

    @GetMapping("/book")
    public String bookForm(Model model) {
        model.addAttribute("doctors", userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.DOCTOR)
                .toList());
        model.addAttribute("appointment", new Appointment());
        return "appointment/book";
    }

    @PostMapping("/book")
    public String book(@ModelAttribute Appointment appointment,
                       @RequestParam Long doctorId,
                       @RequestParam String date,
                       @RequestParam String time,
                       @RequestParam(required = false, defaultValue = "REGULAR") String appointmentType,
                       @RequestParam(required = false) String patientMessage,
                       Principal principal,
                       RedirectAttributes ra) {

        if (principal == null) {
            ra.addFlashAttribute("error", "Please login to book an appointment.");
            return "redirect:/login";
        }

        Patient patient = patientService.findPatientByEmail(principal.getName())
                .orElse(null);

        if (patient == null) {
            ra.addFlashAttribute("error", "Only registered patients can book appointments.");
            return "redirect:/appointment/book";
        }

        User doctor = userRepo.findById(doctorId).orElse(null);
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            ra.addFlashAttribute("error", "Selected doctor was not found.");
            return "redirect:/appointment/book";
        }

        if ("UNAVAILABLE".equalsIgnoreCase(doctor.getAvailableStatus()) || "ON_LEAVE".equalsIgnoreCase(doctor.getAvailableStatus())) {
            ra.addFlashAttribute("error", "Dr. " + doctor.getName() + " is currently " + doctor.getAvailableStatus() + ". Please choose another doctor.");
            return "redirect:/appointment/book";
        }

        LocalDateTime start;
        try {
            start = LocalDateTime.parse(date + "T" + (time.length() == 5 ? time : time.substring(0, 5)));
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Invalid date or time format. Please select valid appointment slot.");
            return "redirect:/appointment/book";
        }

        if (start.isBefore(LocalDateTime.now())) {
            ra.addFlashAttribute("error", "Appointment time cannot be in the past. Please select a future date and time.");
            return "redirect:/appointment/book";
        }

        appointment.setStartTime(start);
        appointment.setEndTime(start.plusMinutes(30));
        appointment.setTitle((appointmentType.equalsIgnoreCase("URGENT") ? "🚨 URGENT - " : "Checkup - ") + patient.getName());
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus("SCHEDULED");
        appointment.setPaymentStatus("PENDING");
        appointment.setAmountPaid(doctor.getFees() != null ? doctor.getFees() : 500.0);
        appointment.setAppointmentType(appointmentType);
        appointment.setPatientMessage(patientMessage);

        appointmentRepo.save(appointment);

        ra.addFlashAttribute("msg", "Appointment booked successfully with Dr. " + doctor.getName() + "!");
        return "redirect:/patient/dashboard";
    }

    @GetMapping("/doctor/events")
    @ResponseBody
    public List<?> doctorEvents(Principal principal) {
        if (principal == null) {
            return List.of();
        }
        User doctor = userRepo.findByEmail(principal.getName()).orElseThrow();
        List<Appointment> list = appointmentRepo.findByDoctorIdOrderByStartTimeAsc(doctor.getId());
        return list.stream().map(a -> Map.of(
                "title", a.getTitle() != null ? a.getTitle() : "Appointment",
                "start", a.getStartTime().toString(),
                "end", a.getEndTime().toString(),
                "status", a.getStatus() != null ? a.getStatus() : "SCHEDULED",
                "patientName", a.getPatient() != null ? a.getPatient().getName() : "N/A"
        )).toList();
    }

    @GetMapping("/events")
    @ResponseBody
    public List<?> getEvents(Principal principal) {

        User user = userRepo.findByEmail(principal.getName())
                .orElseThrow();

        Long userId = user.getId();

        List<Appointment> list;

        if (user.getRole().name().equals("PATIENT")) {
            list = appointmentRepo.findByPatientUserIdOrderByStartTimeAsc(userId);
        } 
        else if (user.getRole().name().equals("DOCTOR")) {
            list = appointmentRepo.findByDoctorIdOrderByStartTimeAsc(userId);
        } 
        else {
            list = appointmentRepo.findAll();
        }

        return list.stream().map(a -> Map.of(
                "id", a.getId(),
                "title", a.getTitle() != null ? a.getTitle() : "Appointment",
                "start", a.getStartTime().toString(),
                "end", a.getEndTime().toString(),
                "status", a.getStatus() != null ? a.getStatus() : "SCHEDULED",
                "patientName", a.getPatient() != null ? a.getPatient().getName() : "N/A"
        )).toList();
    }
}