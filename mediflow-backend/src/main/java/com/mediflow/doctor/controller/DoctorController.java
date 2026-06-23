package com.mediflow.doctor.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediflow.doctor.DoctorService;
import com.mediflow.doctor.dto.CreateDoctorRequest;
import com.mediflow.doctor.dto.CreateDoctorResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/doctors")
public class DoctorController {

private final DoctorService doctorService;

public DoctorController(DoctorService doctorService) {
    this.doctorService = doctorService;
}

@PostMapping
public ResponseEntity<CreateDoctorResponse> createDoctor(
    @Valid @RequestBody CreateDoctorRequest request
) {
    CreateDoctorResponse response =
        doctorService.createDoctor(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
}


}
