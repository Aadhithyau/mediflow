package com.mediflow.availability.dto;

import java.time.OffsetDateTime;

public record AvailabilitySlotResponse(
Long id,
Long doctorProfileId,
OffsetDateTime startTime,
OffsetDateTime endTime
) {
}
