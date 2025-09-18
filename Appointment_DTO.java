package com.example.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AppointmentDTO {

    private final Long id;
    private final Long doctorId;
    private final String doctorName;
    private final Long patientId;
    private final String patientName;
    private final String patientEmail;
    private final String patientPhone;
    private final String patientAddress;
    private final LocalDateTime appointmentTime;
    private final int status;

    private final LocalDate appointmentDate;
    private final LocalTime appointmentTimeOnly;
    private final LocalDateTime endTime;

    // Constructor initializing all fields and computing derived values
    public AppointmentDTO(Long id,
                          Long doctorId,
                          String doctorName,
                          Long patientId,
                          String patientName,
                          String patientEmail,
                          String patientPhone,
                          String patientAddress,
                          LocalDateTime appointmentTime,
                          int status) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.patientPhone = patientPhone;
        this.patientAddress = patientAddress;
        this.appointmentTime = appointmentTime;
        this.status = status;

        // Automatically derived values
        this.appointmentDate = appointmentTime.toLocalDate();
        this.appointmentTimeOnly = appointmentTime.toLocalTime();
        this.endTime = appointmentTime.plusHours(1);
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public int getStatus() {
        return status;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getAppointmentTimeOnly() {
        return appointmentTimeOnly;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
