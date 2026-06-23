package com.mediflow.availability.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record CreateAvailabilitySlotRequest(

@NotNull(message = "Start time is required")
@Future(message = "Start time must be in the future")
OffsetDateTime startTime,

@NotNull(message = "End time is required")
@Future(message = "End time must be in the future")
OffsetDateTime endTime

) {
}
