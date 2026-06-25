package com.mediflow.messaging;

import java.util.Objects;

public record AppointmentCompletedMessage(Long appointmentId) {

	public AppointmentCompletedMessage {
		Objects.requireNonNull(appointmentId, "appointmentId is required");
	}

}
