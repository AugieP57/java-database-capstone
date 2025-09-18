package com.example.service;

import com.example.model.Appointment;
import com.example.model.Doctor;
import com.example.model.Patient;
import com.example.repository.AppointmentRepository;
import com.example.repository.DoctorRepository;
import com.example.repository.PatientRepository;
import com.example.security.TokenService; // adjust package as needed

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              TokenService tokenService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
    }

    /**
     * Books a new appointment.
     * @return 1 on success, 0 on failure
     */
    public int bookAppointment(Appointment appointment) {
        try {
            // Basic validation (extend as needed)
            Map<String, String> errors = validateAppointment(appointment);
            if (!errors.isEmpty()) return 0;

            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Updates an existing appointment (validates before saving).
     */
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> body = new HashMap<>();

        if (appointment == null || appointment.getId() == null) {
            body.put("message", "Invalid request: appointment or ID missing.");
            return ResponseEntity.badRequest().body(body);
        }

        return appointmentRepository.findById(appointment.getId())
                .map(existing -> {
                    // Validate update
                    Map<String, String> errors = validateAppointment(appointment);
                    if (!errors.isEmpty()) {
                        body.putAll(errors);
                        return ResponseEntity.badRequest().body(body);
                    }

                    // Persist update
                    appointmentRepository.save(appointment);
                    body.put("message", "Appointment updated successfully.");
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> {
                    body.put("message", "Appointment not found.");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
                });
    }

    /**
     * Cancels an appointment if the requester (from token) is the booking patient.
     */
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> body = new HashMap<>();

        Optional<Appointment> opt = appointmentRepository.findById(id);
        if (opt.isEmpty()) {
            body.put("message", "Appointment not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        Appointment appt = opt.get();

        // Ensure the patient who booked it is canceling (adjust role logic as needed)
        Long requesterId = tokenService.getUserIdFromToken(token);     // adapt to your TokenService
        String role = tokenService.getRoleFromToken(token);            // e.g., "patient", "doctor", "admin"

        if (!"patient".equalsIgnoreCase(role) || appt.getPatient() == null
                || !Objects.equals(appt.getPatient().getId(), requesterId)) {
            body.put("message", "You are not authorized to cancel this appointment.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }

        appointmentRepository.delete(appt);
        body.put("message", "Appointment canceled successfully.");
        return ResponseEntity.ok(body);
    }

    /**
     * Retrieves appointments for the doctor (from token) on a specific date,
     * optionally filtered by patient name (case-insensitive, partial match).
     *
     * Returns: { "appointments": List<Appointment> }
     */
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> result = new HashMap<>();

        // Expecting the token to belong to the doctor (or include doctorId). Adjust if your flow differs.
        Long doctorId = tokenService.getUserIdFromToken(token);

        // Build [start, end) of the given date
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<Appointment> appts;
        if (pname != null && !pname.trim().isEmpty()) {
            appts = appointmentRepository
                    .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                            doctorId, pname.trim(), start, end);
        } else {
            appts = appointmentRepository
                    .findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);
        }

        result.put("appointments", appts);
        return result;
    }

    // ------------------------------------------------------------
    // Helper validation (expand with overlap checks, business rules, etc.)
    // ------------------------------------------------------------
    private Map<String, String> validateAppointment(Appointment appt) {
        Map<String, String> errors = new HashMap<>();
        if (appt == null) {
            errors.put("message", "Appointment cannot be null.");
            return errors;
        }
        if (appt.getAppointmentTime() == null) {
            errors.put("message", "Appointment time is required.");
            return errors;
        }

        // Validate doctor exists
        if (appt.getDoctor() == null || appt.getDoctor().getId() == null) {
            errors.put("message", "Doctor information is required.");
            return errors;
        } else {
            Optional<Doctor> doc = doctorRepository.findById(appt.getDoctor().getId());
            if (doc.isEmpty()) {
                errors.put("message", "Invalid doctor ID.");
                return errors;
            }
        }

        // Validate patient exists
        if (appt.getPatient() == null || appt.getPatient().getId() == null) {
            errors.put("message", "Patient information is required.");
            return errors;
        } else {
            Optional<Patient> pat = patientRepository.findById(appt.getPatient().getId());
            if (pat.isEmpty()) {
                errors.put("message", "Invalid patient ID.");
                return errors;
            }
        }

        // TODO: add overlap/conflict checks here if your domain requires it
        // e.g., use appointmentRepository to query overlapping slots for the same doctor/patient.

        return errors;
    }
}
