package com.mediflow.doctor.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateDoctorAccountStatusRequest(

@NotNull(message = "Enabled status is required")
Boolean enabled

) {
}