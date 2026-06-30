package com.mediflow.appointment;

import java.util.Objects;

public record AppointmentCancelledEvent(
Long appointmentId
) {

public AppointmentCancelledEvent {
    Objects.requireNonNull(
        appointmentId,
        "appointmentId is required"
    );
}

}