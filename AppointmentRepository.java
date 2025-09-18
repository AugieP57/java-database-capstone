package com.example.repository;

import com.example.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // 1) Retrieve appointments for a doctor within a time range, loading doctor + availability
    @Query("""
           SELECT a
           FROM Appointment a
           LEFT JOIN FETCH a.doctor d
           LEFT JOIN FETCH d.availability av
           WHERE d.id = :doctorId
             AND a.appointmentTime BETWEEN :start AND :end
           ORDER BY a.appointmentTime ASC
           """)
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(@Param("doctorId") Long doctorId,
                                                              @Param("start") LocalDateTime start,
                                                              @Param("end") LocalDateTime end);

    // 2) Filter by doctor ID, partial patient name (case-insensitive), and time range
    //    Load patient + doctor details
    @Query("""
           SELECT a
           FROM Appointment a
           LEFT JOIN FETCH a.patient p
           LEFT JOIN FETCH a.doctor d
           WHERE d.id = :doctorId
             AND LOWER(p.name) LIKE LOWER(CONCAT('%', :patientName, '%'))
             AND a.appointmentTime BETWEEN :start AND :end
           ORDER BY a.appointmentTime ASC
           """)
    List<Appointment> findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("patientName") String patientName,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 3) Delete all appointments for a specific doctor
    @Modifying
    @Transactional
    @Query("DELETE FROM Appointment a WHERE a.doctor.id = :doctorId")
    void deleteAllByDoctorId(@Param("doctorId") Long doctorId);

    // 4) Find all appointments for a specific patient
    //    (Use explicit JPQL to support relation-based patient.id)
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId ORDER BY a.appointmentTime ASC")
    List<Appointment> findByPatientId(@Param("patientId") Long patientId);

    // 5) Retrieve appointments for a patient by status, ordered by appointment time
    List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(Long patientId, int status);

    // 6) Case-insensitive partial doctor name + patient id
    @Query("""
           SELECT a
           FROM Appointment a
           JOIN a.doctor d
           WHERE a.patient.id = :patientId
             AND LOWER(d.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))
           ORDER BY a.appointmentTime ASC
           """)
    List<Appointment> filterByDoctorNameAndPatientId(@Param("doctorName") String doctorName,
                                                     @Param("patientId") Long patientId);

    // 7) Case-insensitive partial doctor name + patient id + status
    @Query("""
           SELECT a
           FROM Appointment a
           JOIN a.doctor d
           WHERE a.patient.id = :patientId
             AND a.status = :status
             AND LOWER(d.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))
           ORDER BY a.appointmentTime ASC
           """)
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(@Param("doctorName") String doctorName,
                                                              @Param("patientId") Long patientId,
                                                              @Param("status") int status);
}

