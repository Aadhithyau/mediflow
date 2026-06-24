package com.mediflow.appointment.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediflow.appointment.AppointmentService;
import com.mediflow.appointment.dto.DoctorAppointmentResponse;

@RestController
@RequestMapping("/api/doctor/appointments")
public class DoctorAppointmentController {

    private final AppointmentService appointmentService;

    public DoctorAppointmentController(
        AppointmentService appointmentService
    ) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<DoctorAppointmentResponse>>
        getDoctorAppointments(
            @AuthenticationPrincipal Jwt jwt
        ) {
        List<DoctorAppointmentResponse> appointments =
            appointmentService.getDoctorAppointments(
                jwt.getSubject()
            );

        return ResponseEntity.ok(appointments);
    }

    @PatchMapping("/{appointmentId}/complete")
    public ResponseEntity<DoctorAppointmentResponse>
        completeAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long appointmentId
        ) {
        DoctorAppointmentResponse response =
            appointmentService.completeAppointment(
                jwt.getSubject(),
                appointmentId
            );

        return ResponseEntity.ok(response);
    }
}