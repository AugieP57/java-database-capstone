package com.example.service;

import com.example.dto.AppointmentDTO;
import com.example.model.Appointment;
import com.example.model.Patient;
import com.example.repository.AppointmentRepository;
import com.example.repository.PatientRepository;
import com.example.security.TokenService; // adjust package if needed

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    /**
     * 1) Saves a new patient.
     * @return 1 on success, 0 on failure
     */
    public int createPatient(Patient patient) {
        try {
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 2) Retrieves appointments for a specific patient (caller verified by email from token).
     * On mismatch between token email->patient and provided id, returns 401.
     */
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        Map<String, Object> body = new HashMap<>();

        if (id == null || token == null || token.isBlank()) {
            body.put("message", "Invalid request.");
            return ResponseEntity.badRequest().body(body);
        }

        String email = tokenService.getEmailFromToken(token);
        if (email == null || email.isBlank()) {
            body.put("message", "Unauthorized: invalid token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        Patient tokenPatient = patientRepository.findByEmail(email);
        if (tokenPatient == null || !Objects.equals(tokenPatient.getId(), id)) {
            body.put("message", "Unauthorized: token does not match requested patient.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        List<Appointment> appts = appointmentRepository.findByPatientId(id);
        List<AppointmentDTO> dtoList = appts.stream().map(this::toDTO).collect(Collectors.toList());

        body.put("appointments", dtoList);
        return ResponseEntity.ok(body);
    }

    /**
     * 3) Filters appointments by condition ("past" or "future") for a patient.
     * Spec says: status 1 = past, status 0 = future.
     */
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Map<String, Object> body = new HashMap<>();
        if (id == null || condition == null) {
            body.put("message", "Invalid request.");
            return ResponseEntity.badRequest().body(body);
        }

        int status;
        String c = condition.trim().toLowerCase(Locale.ROOT);
        if ("past".equals(c)) {
            status = 1;
        } else if ("future".equals(c)) {
            status = 0;
        } else {
            body.put("message", "Condition must be 'past' or 'future'.");
            return ResponseEntity.badRequest().body(body);
        }

        List<Appointment> appts =
                appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status);
        List<AppointmentDTO> dtoList = appts.stream().map(this::toDTO).collect(Collectors.toList());

        body.put("appointments", dtoList);
        return ResponseEntity.ok(body);
    }

    /**
     * 4) Filters a patient's appointments by doctor's name (partial, case-insensitive).
     */
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        Map<String, Object> body = new HashMap<>();
        if (patientId == null) {
            body.put("message", "Invalid patient id.");
            return ResponseEntity.badRequest().body(body);
        }
        String doctorName = name == null ? "" : name.trim();

        List<Appointment> appts =
                appointmentRepository.filterByDoctorNameAndPatientId(doctorName, patientId);
        List<AppointmentDTO> dtoList = appts.stream().map(this::toDTO).collect(Collectors.toList());

        body.put("appointments", dtoList);
        return ResponseEntity.ok(body);
    }

    /**
     * 5) Filters a patient's appointments by doctor's name AND condition ("past"/"future").
     * Uses status 1 = past, 0 = future.
     */
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name, long patientId) {
        Map<String, Object> body = new HashMap<>();
        String c = condition == null ? "" : condition.trim().toLowerCase(Locale.ROOT);
        int status;
        if ("past".equals(c)) status = 1;
        else if ("future".equals(c)) status = 0;
        else {
            body.put("message", "Condition must be 'past' or 'future'.");
            return ResponseEntity.badRequest().body(body);
        }

        String doctorName = name == null ? "" : name.trim();

        List<Appointment> appts =
                appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(doctorName, patientId, status);
        List<AppointmentDTO> dtoList = appts.stream().map(this::toDTO).collect(Collectors.toList());

        body.put("appointments", dtoList);
        return ResponseEntity.ok(body);
    }

    /**
     * 6) Fetch patient details from token (by email).
     * Returns the patient with sensitive fields (e.g., password) nulled out.
     */
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        Map<String, Object> body = new HashMap<>();
        if (token == null || token.isBlank()) {
            body.put("message", "Invalid token.");
            return ResponseEntity.badRequest().body(body);
        }

        String email = tokenService.getEmailFromToken(token);
        if (email == null || email.isBlank()) {
            body.put("message", "Unauthorized: invalid token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        Patient patient = patientRepository.findByEmail(email);
        if (patient == null) {
            body.put("message", "Patient not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // Safety: don't return password or other sensitive info
        try {
            patient.setPassword(null);
        } catch (Exception ignored) {
            // if Patient has no password field setter, ignore
        }

        body.put("patient", patient);
        return ResponseEntity.ok(body);
    }

    // -----------------------------
    // Helpers
    // -----------------------------

    private AppointmentDTO toDTO(Appointment a) {
        Long id = a.getId();
        Long doctorId = a.getDoctor() != null ? a.getDoctor().getId() : null;
        String doctorName = a.getDoctor() != null ? a.getDoctor().getName() : null;
        Long patientId = a.getPatient() != null ? a.getPatient().getId() : null;
        String patientName = a.getPatient() != null ? a.getPatient().getName() : null;
        String patientEmail = a.getPatient() != null ? a.getPatient().getEmail() : null;
        String patientPhone = a.getPatient() != null ? a.getPatient().getPhone() : null;
        String patientAddress = a.getPatient() != null ? a.getPatient().getAddress() : null;
        LocalDateTime appointmentTime = a.getAppointmentTime();
        int status = a.getStatus();

        return new AppointmentDTO(
                id,
                doctorId,
                doctorName,
                patientId,
                patientName,
                patientEmail,
                patientPhone,
                patientAddress,
                appointmentTime,
                status
        );
    }
}
