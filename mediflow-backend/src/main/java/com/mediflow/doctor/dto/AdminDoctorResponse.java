package com.mediflow.doctor.dto;

import java.math.BigDecimal;

public record AdminDoctorResponse(

Long doctorProfileId,

Long userId,

String fullName,

String email,

boolean enabled,

String specialization,

String medicalLicenseNumber,

BigDecimal consultationFee,

String hospitalName,

String hospitalAddress,

String bio

) {
}