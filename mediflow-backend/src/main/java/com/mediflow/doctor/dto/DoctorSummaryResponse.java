package com.mediflow.doctor.dto;

import java.math.BigDecimal;

public record DoctorSummaryResponse(
Long doctorProfileId,
Long userId,
String fullName,
String specialization,
BigDecimal consultationFee,
String hospitalName,
String hospitalAddress,
String bio
) {
}
