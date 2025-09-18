package com.example.service;

import com.example.model.Admin;
import com.example.model.Doctor;
import com.example.model.Patient;
import com.example.repository.AdminRepository;
import com.example.repository.DoctorRepository;
import com.example.repository.PatientRepository;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    // secret loaded from application.properties: jwt.secret=some-very-long-secret-key
    @Value("${jwt.secret}")
    private String secret;

    private SecretKey signingKey;

    public TokenService(AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @PostConstruct
    private void initKey() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ------------------------------------------------------------
    // generateToken
    // ------------------------------------------------------------
    /**
     * Generates a JWT token for a user with a 7-day expiration.
     * @param identifier username (admin) or email (doctor/patient)
     */
    public String generateToken(String identifier) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000); // 7 days

        return Jwts.builder()
                .setSubject(identifier)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ------------------------------------------------------------
    // extractIdentifier
    // ------------------------------------------------------------
    /**
     * Extracts the identifier (username/email) from a JWT token.
     */
    public String extractIdentifier(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    // ------------------------------------------------------------
    // validateToken
    // ------------------------------------------------------------
    /**
     * Validates a JWT token and checks if the corresponding user exists.
     * @param token the JWT
     * @param userType "admin", "doctor", or "patient"
     * @return true if token valid and user exists
     */
    public boolean validateToken(String token, String userType) {
        try {
            String identifier = extractIdentifier(token);
            if (identifier == null) return false;

            return switch (userType.toLowerCase()) {
                case "admin" -> adminRepository.findByUsername(identifier) != null;
                case "doctor" -> doctorRepository.findByEmail(identifier) != null;
                case "patient" -> patientRepository.findByEmail(identifier) != null;
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------
    // getSigningKey
    // ------------------------------------------------------------
    /**
     * Exposes signing key (mainly for tests).
     */
    public SecretKey getSigningKey() {
        return signingKey;
    }
}
