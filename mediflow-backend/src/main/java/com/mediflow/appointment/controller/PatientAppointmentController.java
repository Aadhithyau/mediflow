package com.mediflow.appointment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediflow.appointment.AppointmentService;
import com.mediflow.appointment.dto.AppointmentResponse;
import com.mediflow.appointment.dto.CreateAppointmentRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
public class PatientAppointmentController {

    private final AppointmentService appointmentService;

    public PatientAppointmentController(
        AppointmentService appointmentService
    ) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> bookAppointment(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateAppointmentRequest request
    ) {
        AppointmentResponse response =
            appointmentService.bookAppointment(
                jwt.getSubject(),
                request
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
}