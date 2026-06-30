package com.mediflow.messaging;

import java.util.Objects;

public record AppointmentReminderMessage(

Long appointmentId,

AppointmentEmailRecipient recipient

) {

public AppointmentReminderMessage {
    Objects.requireNonNull(
        appointmentId,
        "appointmentId is required"
    );

    Objects.requireNonNull(
        recipient,
        "recipient is required"
    );
}

}