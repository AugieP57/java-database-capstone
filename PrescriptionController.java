
package com.example.controller;

import com.example.model.Prescription;
import com.example.service.PrescriptionService;
import com.example.service.Service; // your central validation/service class

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.path}" + "prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final Service service;

    public PrescriptionController(PrescriptionService prescriptionService, Service service) {
        this.prescriptionService = prescriptionService;
        this.service = service;
    }

    /**
     * 1) Save Prescription
     * POST /prescription/{token}
     * Only doctors can save prescriptions.
     */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> savePrescription(@PathVariable String token,
                                                                @RequestBody Prescription prescription) {
        // Validate token for doctor role
        var validation = service.validateToken(token, "doctor");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }

        int result = prescriptionService.savePrescription(prescription); // expected: 1=success, 0=error
        if (result == 1) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Prescription saved successfully"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to save prescription"));
    }

    /**
     * 2) Get Prescription by Appointment ID
     * GET /prescription/{appointmentId}/{token}
     * Only doctors can fetch prescriptions by appointment.
     */
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> getByAppointment(@PathVariable Long appointmentId,
                                              @PathVariable String token) {
        // Validate token for doctor role
        var validation = service.validateToken(token, "doctor");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }

        List<Prescription> prescriptions = prescriptionService.getPrescription(appointmentId);
        if (prescriptions == null || prescriptions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No prescription exists for this appointment"));
        }
        return ResponseEntity.ok(Map.of("prescriptions", prescriptions));
    }
}
