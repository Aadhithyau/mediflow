package com.mediflow.availability.controller;

import org.springframework.http.HttpStatus;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import com.mediflow.availability.AvailabilityService;
import com.mediflow.availability.dto.AvailabilitySlotResponse;
import com.mediflow.availability.dto.CreateAvailabilitySlotRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/doctor/availability-slots")
public class DoctorAvailabilityController {

private final AvailabilityService availabilityService;

public DoctorAvailabilityController(
    AvailabilityService availabilityService
) {
    this.availabilityService = availabilityService;
}

@GetMapping
public ResponseEntity<List<AvailabilitySlotResponse>>
getOwnFutureSlots(
@AuthenticationPrincipal Jwt jwt
) {

return ResponseEntity.ok(
    availabilityService.getOwnFutureSlots(
        jwt.getSubject()
    )
);

}


@PostMapping
public ResponseEntity<AvailabilitySlotResponse> createSlot(
    @AuthenticationPrincipal Jwt jwt,
    @Valid @RequestBody CreateAvailabilitySlotRequest request
) {
    AvailabilitySlotResponse response =
        availabilityService.createSlot(
            jwt.getSubject(),
            request
        );

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
}

}
