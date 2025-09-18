package com.example.service;

import com.example.model.Doctor;
import com.example.model.Appointment;
import com.example.repository.DoctorRepository;
import com.example.repository.AppointmentRepository;
import com.example.security.TokenService;            // adjust package as needed
import com.example.model.Login;                     // if you named it LoginRequestDTO, just change the type here

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    private static final DateTimeFormatter SLOT_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    /**
     * Fetch available slots for a doctor on a given date.
     * Assumes Doctor has a list of daily availability time strings like "09:00", "09:30", ...
     * Booked slots (appointments) are removed from that set.
     */
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Optional<Doctor> opt = doctorRepository.findById(doctorId);
        if (opt.isEmpty()) return Collections.emptyList();

        Doctor doctor = opt.get();

        // Base available slots from the doctor profile (null-safe)
        List<String> baseSlots = Optional.ofNullable(doctor.getAvailability())
                                         .orElse(Collections.emptyList());

        if (baseSlots.isEmpty()) return Collections.emptyList();

        // Window for that day
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        // Booked appointments for that day
        List<Appointment> appts = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);

        // Collect booked times as "HH:mm"
        Set<String> booked = appts.stream()
                .map(a -> a.getAppointmentTime().toLocalTime().format(SLOT_FMT))
                .collect(Collectors.toSet());

        // Filter out booked
        return baseSlots.stream()
                .filter(s -> !booked.contains(s))
                .sorted(Comparator.comparing(s -> LocalTime.parse(s, SLOT_FMT)))
                .collect(Collectors.toList());
    }

    /**
     * Save a new doctor.
     * @return 1 = success, -1 = already exists (by email), 0 = internal error
     */
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctor == null || doctor.getEmail() == null) return 0;

            Doctor existing = doctorRepository.findByEmail(doctor.getEmail());
            if (existing != null) return -1;

            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Update an existing doctor.
     * @return 1 = success, -1 = not found, 0 = internal error
     */
    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor == null || doctor.getId() == null) return 0;

            return doctorRepository.findById(doctor.getId())
                    .map(d -> {
                        // preserve ID; save all updated fields coming from 'doctor'
                        doctorRepository.save(doctor);
                        return 1;
                    })
                    .orElse(-1);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get all doctors.
     */
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    /**
     * Delete a doctor and cascade delete their appointments first.
     * @return 1 = success, -1 = not found, 0 = internal error
     */
    public int deleteDoctor(long id) {
        try {
            if (!doctorRepository.existsById(id)) return -1;
            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Validate a doctor's login and return token on success.
     * If your auth stores hashed passwords, replace equals(...) with a PasswordEncoder check.
     */
    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> body = new HashMap<>();

        if (login == null || login.getEmail() == null || login.getPassword() == null) {
            body.put("message", "Email and password are required.");
            return ResponseEntity.badRequest().body(body);
        }

        Doctor doc = doctorRepository.findByEmail(login.getEmail());
        if (doc == null) {
            body.put("message", "Invalid email or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        // TODO: replace with PasswordEncoder if you hash passwords
        if (!Objects.equals(doc.getPassword(), login.getPassword())) {
            body.put("message", "Invalid email or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        // Generate token (adjust method to match your TokenService)
        String token = tokenService.generateToken(doc.getId(), "doctor");
        body.put("token", token);
        body.put("message", "Login successful.");
        return ResponseEntity.ok(body);
    }

    /**
     * Find doctors by (partial) name.
     */
    public Map<String, Object> findDoctorByName(String name) {
        List<Doctor> docs = doctorRepository.findByNameLike(name == null ? "" : name.trim());
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", docs);
        return result;
    }

    /**
     * Filter by name, specialty, and AM/PM availability.
     */
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        List<Doctor> docs = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                        safe(name), safe(specialty)
                );
        List<Doctor> filtered = filterDoctorByTime(docs, amOrPm);
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", filtered);
        return result;
    }

    /**
     * Filter by name and AM/PM availability.
     */
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        List<Doctor> docs = doctorRepository.findByNameLike(safe(name));
        List<Doctor> filtered = filterDoctorByTime(docs, amOrPm);
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", filtered);
        return result;
    }

    /**
     * Filter by name and specialty.
     */
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specilty) {
        List<Doctor> docs = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                        safe(name), safe(specilty)
                );
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", docs);
        return result;
    }

    /**
     * Filter by specialty and AM/PM availability.
     */
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specilty, String amOrPm) {
        List<Doctor> docs = doctorRepository.findBySpecialtyIgnoreCase(safe(specilty));
        List<Doctor> filtered = filterDoctorByTime(docs, amOrPm);
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", filtered);
        return result;
    }

    /**
     * Filter by specialty only.
     */
    public Map<String, Object> filterDoctorBySpecility(String specilty) {
        List<Doctor> docs = doctorRepository.findBySpecialtyIgnoreCase(safe(specilty));
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", docs);
        return result;
    }

    /**
     * Filter all doctors by AM/PM availability.
     */
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        List<Doctor> docs = doctorRepository.findAll();
        List<Doctor> filtered = filterDoctorByTime(docs, amOrPm);
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", filtered);
        return result;
    }

    /**
     * PRIVATE: From a list of doctors, keep only those with availability matching AM/PM.
     * Availability is assumed to be List<String> "HH:mm". If a doctor has any slot in the half-day, they pass.
     */
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        if (doctors == null || doctors.isEmpty() || amOrPm == null) return Collections.emptyList();
        String half = amOrPm.trim().toUpperCase(Locale.ROOT);
        boolean wantAM = "AM".equals(half);
        boolean wantPM = "PM".equals(half);

        if (!wantAM && !wantPM) return Collections.emptyList();

        return doctors.stream()
                .filter(d -> {
                    List<String> slots = Optional.ofNullable(d.getAvailability()).orElse(Collections.emptyList());
                    for (String s : slots) {
                        try {
                            LocalTime t = LocalTime.parse(s, SLOT_FMT);
                            if (wantAM && t.isBefore(LocalTime.NOON)) return true;
                            if (wantPM && !t.isBefore(LocalTime.NOON)) return true; // noon and after = PM
                        } catch (Exception ignored) {
                            // If a slot isn't parseable as HH:mm, skip it
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
