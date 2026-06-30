package com.mediflow.appointment;

import java.util.Objects;

public record AppointmentBookedEvent(
Long appointmentId
) {

public AppointmentBookedEvent {
    Objects.requireNonNull(
        appointmentId,
        "appointmentId is required"
    );
}

}