package com.example.dto;

public class LoginRequestDTO {

    private String identifier;
    private String password;

    // Default constructor (needed for deserialization)
    public LoginRequestDTO() {
    }

    // Parameterized constructor for convenience
    public LoginRequestDTO(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    // Getters and Setters
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

