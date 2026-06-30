package com.mediflow.appointment;

import java.util.Objects;

public record AppointmentReminderEvent(
Long appointmentId
) {

public AppointmentReminderEvent {
    Objects.requireNonNull(
        appointmentId,
        "appointmentId is required"
    );
}

}