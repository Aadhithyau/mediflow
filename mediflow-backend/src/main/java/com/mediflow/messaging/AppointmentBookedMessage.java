package com.mediflow.messaging;

import java.util.Objects;

public record AppointmentBookedMessage(

Long appointmentId,

AppointmentEmailRecipient recipient

) {

public AppointmentBookedMessage {
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