package com.mediflow.doctor.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDoctorRequest(

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100,
        message = "Full name must contain between 2 and 100 characters")
    String fullName,

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150,
        message = "Email must not exceed 150 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72,
        message = "Password must contain between 8 and 72 characters")
    String password,

    @NotBlank(message = "Specialization is required")
    @Size(max = 100,
        message = "Specialization must not exceed 100 characters")
    String specialization,

    @NotBlank(message = "Medical license number is required")
    @Size(max = 100,
        message = "Medical license number must not exceed 100 characters")
    String medicalLicenseNumber,

    @NotNull(message = "Consultation fee is required")
    @DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "Consultation fee cannot be negative"
    )
    @Digits(
        integer = 8,
        fraction = 2,
        message = "Consultation fee must contain at most 8 whole digits and 2 decimal places"
    )
    BigDecimal consultationFee,

    @NotBlank(message = "Hospital name is required")
    @Size(max = 150,
        message = "Hospital name must not exceed 150 characters")
    String hospitalName,

    @NotBlank(message = "Hospital address is required")
    @Size(max = 500,
        message = "Hospital address must not exceed 500 characters")
    String hospitalAddress,

    @Size(max = 1000,
        message = "Bio must not exceed 1000 characters")
    String bio

) {
}

