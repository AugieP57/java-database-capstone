package com.example.service;

import com.example.model.Admin;
import com.example.model.Appointment;
import com.example.model.Doctor;
import com.example.model.Patient;
import com.example.model.Login; // if you named it LoginRequestDTO, change the import/type here

import com.example.repository.AdminRepository;
import com.example.repository.DoctorRepository;
import com.example.repository.PatientRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class Service {

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    private static final DateTimeFormatter SLOT_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public Service(TokenService tokenService,
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   DoctorService doctorService,
                   PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    // ------------------------------------------------------------
    // validateToken
    // ------------------------------------------------------------
    /**
     * Checks validity of a token for a given user role.
     * Returns 401 with an error message if invalid/expired.
     * If valid, returns 200 OK with an empty body (or a simple message).
     */
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> body = new HashMap<>();

        if (token == null || token.isBlank() || user == null || user.isBlank()) {
            body.put("message", "Invalid token or user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        boolean valid = tokenService.validateToken(token, user);
        if (!valid) {
            body.put("message", "Token is invalid or expired.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        // Valid -> return 200; you can also add { "message": "valid" } if desired
        return ResponseEntity.ok(body);
    }

    // ------------------------------------------------------------
    // validateAdmin
    // ------------------------------------------------------------
    /**
     * Validates admin credentials and returns a token on success.
     */
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> body = new HashMap<>();

        if (receivedAdmin == null || receivedAdmin.getUsername() == null || receivedAdmin.getPassword() == null) {
            body.put("message", "Username and password are required.");
            return ResponseEntity.badRequest().body(body);
        }

        Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
        if (admin == null) {
            body.put("message", "Invalid username or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        // TODO: replace with PasswordEncoder matches if you hash admin passwords
        if (!Objects.equals(admin.getPassword(), receivedAdmin.getPassword())) {
            body.put("message", "Invalid username or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        String token = tokenService.generateToken(admin.getId(), "admin");
        body.put("token", token);
        body.put("message", "Login successful.");
        return ResponseEntity.ok(body);
    }

    // ------------------------------------------------------------
    // filterDoctor
    // ------------------------------------------------------------
    /**
     * Filters doctors by name, specialty, and available AM/PM time.
     * Delegates to DoctorService for the appropriate combination.
     */
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = name != null && !name.trim().isEmpty();
        boolean hasSpec = specialty != null && !specialty.trim().isEmpty();
        boolean hasTime = time != null && !time.trim().isEmpty();

        if (hasName && hasSpec && hasTime) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        } else if (hasName && hasTime) {
            return doctorService.filterDoctorByNameAndTime(name, time);
        } else if (hasName && hasSpec) {
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        } else if (hasSpec && hasTime) {
            return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        } else if (hasSpec) {
            return doctorService.filterDoctorBySpecility(specialty);
        } else if (hasTime) {
            return doctorService.filterDoctorsByTime(time);
        } else if (hasName) {
            return doctorService.findDoctorByName(name);
        } else {
            // No filters: return all doctors
            Map<String, Object> result = new HashMap<>();
            result.put("doctors", doctorService.getDoctors());
            return result;
        }
    }

    // ------------------------------------------------------------
    // validateAppointment
    // ------------------------------------------------------------
    /**
     * Validates whether an appointment time is available for the given doctor.
     * @return 1 = valid time, 0 = unavailable time, -1 = doctor doesn't exist
     */
    public int validateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getDoctor() == null || appointment.getDoctor().getId() == null
                || appointment.getAppointmentTime() == null) {
            return 0;
        }

        Long docId = appointment.getDoctor().getId();
        Optional<Doctor> docOpt = doctorRepository.findById(docId);
        if (docOpt.isEmpty()) return -1;

        var date = appointment.getAppointmentTime().toLocalDate();
        var timeStr = appointment.getAppointmentTime().toLocalTime().format(SLOT_FMT);

        List<String> available = doctorService.getDoctorAvailability(docId, date);
        return available.contains(timeStr) ? 1 : 0;
    }

    // ------------------------------------------------------------
    // validatePatient
    // ------------------------------------------------------------
    /**
     * Checks if a patient exists by email or phone.
     * @return true if patient does NOT exist (safe to create), false if already exists.
     */
    public boolean validatePatient(Patient patient) {
        if (patient == null) return false;
        String email = safe(patient.getEmail());
        String phone = safe(patient.getPhone());

        // If both are empty, treat as invalid input -> "exists" to block creation
        if (email.isEmpty() && phone.isEmpty()) return false;

        Patient existing = patientRepository.findByEmailOrPhone(
                email.isEmpty() ? null : email,
                phone.isEmpty() ? null : phone
        );
        return existing == null; // true means not found -> valid to create
    }

    // ------------------------------------------------------------
    // validatePatientLogin
    // ------------------------------------------------------------
    /**
     * Validates patient login and issues a token.
     */
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> body = new HashMap<>();
        if (login == null || login.getEmail() == null || login.getPassword() == null) {
            body.put("message", "Email and password are required.");
            return ResponseEntity.badRequest().body(body);
        }

        Patient patient = patientRepository.findByEmail(login.getEmail());
        if (patient == null) {
            body.put("message", "Invalid email or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        // TODO: replace with PasswordEncoder if using hashes
        if (!Objects.equals(patient.getPassword(), login.getPassword())) {
            body.put("message", "Invalid email or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        String token = tokenService.generateToken(patient.getId(), "patient");
        body.put("token", token);
        body.put("message", "Login successful.");
        return ResponseEntity.ok(body);
    }

    // ------------------------------------------------------------
    // filterPatient
    // ------------------------------------------------------------
    /**
     * Filters patient appointments by condition and/or doctor name.
     * Delegates to PatientService; expects token to identify the patient (by email).
     */
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        // Resolve patient by token email
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid token."));
        }
        String email = tokenService.getEmailFromToken(token);
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized: invalid token."));
        }
        Patient patient = patientRepository.findByEmail(email);
        if (patient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Patient not found."));
        }
        Long patientId = patient.getId();

        boolean hasCond = condition != null && !condition.trim().isEmpty();
        boolean hasName = name != null && !name.trim().isEmpty();

        if (hasCond && hasName) {
            return patientService.filterByDoctorAndCondition(condition.trim(), name.trim(), patientId);
        } else if (hasCond) {
            return patientService.filterByCondition(condition.trim(), patientId);
        } else if (hasName) {
            return patientService.filterByDoctor(name.trim(), patientId);
        } else {
            // No filters: return all appointments for this patient
            // We can reuse patientService.getPatientAppointment to fetch everything safely.
            return patientService.getPatientAppointment(patientId, token);
        }
    }

    // ------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
