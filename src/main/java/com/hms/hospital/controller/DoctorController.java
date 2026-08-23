package com.hms.hospital.controller;
import com.hms.hospital.entity.Appointment;
import com.hms.hospital.entity.User;
import com.hms.hospital.repository.AppointmentRepository;
import com.hms.hospital.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import java.time.LocalTime;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class DoctorController {

    private final UserRepository userRepo;
    private final AppointmentRepository appointmentRepo;

    @GetMapping("/doctor/dashboard")
    public String doctorDashboard(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User doctor = userRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Appointment> todayAppointments = appointmentRepo
                .findByDoctorIdAndStartTimeBetween(doctor.getId(), startOfDay, endOfDay);

        List<Appointment> todaysCompletedAppointments = appointmentRepo
                .findByDoctorIdAndStatusAndStartTimeBetween(doctor.getId(), "COMPLETED", startOfDay, endOfDay);

        List<Appointment> upcomingAppointments = appointmentRepo
                .findByDoctorIdAndStartTimeAfter(doctor.getId(), LocalDateTime.now());

        List<Appointment> allAppointments = appointmentRepo
                .findByDoctorIdOrderByStartTimeAsc(doctor.getId());

        model.addAttribute("doctor", doctor);
        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("todaysCompletedAppointments", todaysCompletedAppointments);
        model.addAttribute("totalTodayCompleted", todaysCompletedAppointments.size());
        model.addAttribute("totalToday", todayAppointments.size());
        model.addAttribute("allAppointments", allAppointments);
        model.addAttribute("upcomingAppointments", upcomingAppointments);

        return "doctor/dashboard";
    }

    @PostMapping("/doctor/status")
    public String updateDoctorStatus(@RequestParam String availableStatus, Principal principal, RedirectAttributes ra) {
        if (principal != null) {
            User doctor = userRepo.findByEmail(principal.getName()).orElse(null);
            if (doctor != null) {
                doctor.setAvailableStatus(availableStatus);
                userRepo.save(doctor);
                ra.addFlashAttribute("msg", "Your availability status updated to " + availableStatus);
            }
        }
        return "redirect:/doctor/dashboard";
    }

    @PostMapping("/doctor/appointments/{id}/status")
    public String updateAppointmentStatus(@PathVariable Long id, @RequestParam String status, RedirectAttributes ra) {
        Appointment apt = appointmentRepo.findById(id).orElse(null);
        if (apt != null) {
            apt.setStatus(status);
            appointmentRepo.save(apt);
            ra.addFlashAttribute("msg", "Appointment status updated to " + status);
        }
        return "redirect:/doctor/dashboard";
    }
}