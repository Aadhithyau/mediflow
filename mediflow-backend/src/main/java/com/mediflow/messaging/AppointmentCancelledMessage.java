package com.mediflow.messaging;

import java.util.Objects;

public record AppointmentCancelledMessage(

Long appointmentId,

AppointmentEmailRecipient recipient

) {

public AppointmentCancelledMessage {
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