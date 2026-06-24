package com.mediflow.appointment;

import java.util.Objects;

public record AppointmentCompletedEvent(
    Long appointmentId
) {

    public AppointmentCompletedEvent {
        Objects.requireNonNull(
            appointmentId,
            "appointmentId is required"
        );
    }
}