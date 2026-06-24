package com.mediflow.appointment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.mediflow.appointment.AppointmentStatus;

public record DoctorAppointmentResponse(
    Long id,
    Long availabilitySlotId,
    Long patientUserId,
    String patientName,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    BigDecimal consultationFee,
    AppointmentStatus status,
    OffsetDateTime completedAt,
    OffsetDateTime createdAt
) {
}