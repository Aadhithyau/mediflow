package com.mediflow.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAppointmentRequest(

    @NotNull(message = "Availability slot ID is required")
    @Positive(message = "Availability slot ID must be positive")
    Long availabilitySlotId

) {
}