package com.example.repository;

import com.example.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // 1) Find a doctor by email
    Doctor findByEmail(String email);

    // 2) Find doctors by partial name (LIKE + CONCAT for flexible pattern matching)
    @Query("SELECT d FROM Doctor d WHERE d.name LIKE CONCAT('%', :name, '%')")
    List<Doctor> findByNameLike(@Param("name") String name);

    // 3) Filter doctors by partial name and exact specialty (case-insensitive)
    @Query("""
           SELECT d
           FROM Doctor d
           WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))
             AND LOWER(d.specialty) = LOWER(:specialty)
           """)
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(@Param("name") String name,
                                                                      @Param("specialty") String specialty);

    // 4) Find doctors by specialty, ignoring case
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);
}



