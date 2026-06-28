package com.mediflow.doctor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediflow.doctor.DoctorService;
import com.mediflow.doctor.dto.AdminDoctorResponse;
import com.mediflow.doctor.dto.CreateDoctorRequest;
import com.mediflow.doctor.dto.CreateDoctorResponse;
import com.mediflow.doctor.dto.UpdateDoctorAccountStatusRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/doctors")
public class DoctorController {

private final DoctorService doctorService;

public DoctorController(
    DoctorService doctorService
) {
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

@GetMapping
public ResponseEntity<List<AdminDoctorResponse>>
    getAllDoctors() {

    return ResponseEntity.ok(
        doctorService.getAllDoctorsForAdmin()
    );
}

@PatchMapping("/{doctorProfileId}/status")
public ResponseEntity<AdminDoctorResponse>
    updateDoctorAccountStatus(
        @PathVariable Long doctorProfileId,
        @Valid @RequestBody
        UpdateDoctorAccountStatusRequest request
    ) {

    return ResponseEntity.ok(
        doctorService.updateDoctorAccountStatus(
            doctorProfileId,
            request
        )
    );
}

}