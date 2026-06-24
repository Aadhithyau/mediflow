package com.mediflow.appointment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.mediflow.appointment.AppointmentStatus;

public record AppointmentResponse(

Long id,

Long availabilitySlotId,

Long doctorProfileId,

String doctorName,

OffsetDateTime startTime,

OffsetDateTime endTime,

BigDecimal consultationFee,

AppointmentStatus status,

OffsetDateTime createdAt


) {
}
