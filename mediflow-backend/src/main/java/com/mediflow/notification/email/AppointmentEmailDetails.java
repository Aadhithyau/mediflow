package com.mediflow.notification.email;

import java.time.OffsetDateTime;

public record AppointmentEmailDetails(

Long appointmentId,

String patientName,

String patientEmail,

String doctorName,

String doctorEmail,

OffsetDateTime startTime,

OffsetDateTime endTime,

String hospitalName,

String hospitalAddress

) {
}