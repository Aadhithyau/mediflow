package com.mediflow.doctor.dto;

import java.math.BigDecimal;

public record CreateDoctorResponse(
    Long userId,
    Long doctorProfileId,
    String fullName,
    String email,
    String role,
    String specialization,
    String medicalLicenseNumber,
    BigDecimal consultationFee,
    String bio,
    String message
) {
}

