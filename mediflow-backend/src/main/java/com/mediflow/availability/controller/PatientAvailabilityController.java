package com.mediflow.availability.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediflow.availability.AvailabilityService;
import com.mediflow.availability.dto.AvailabilitySlotResponse;

@RestController
@RequestMapping("/api/doctors")
public class PatientAvailabilityController {

    private final AvailabilityService availabilityService;

    public PatientAvailabilityController(
        AvailabilityService availabilityService
    ) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/{doctorProfileId}/availability-slots")
    public ResponseEntity<List<AvailabilitySlotResponse>> getFutureSlots(
        @PathVariable Long doctorProfileId
    ) {
        return ResponseEntity.ok(
            availabilityService.getFutureSlots(doctorProfileId)
        );
    }
}