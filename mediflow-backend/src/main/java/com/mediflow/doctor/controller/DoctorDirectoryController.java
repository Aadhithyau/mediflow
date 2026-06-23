package com.mediflow.doctor.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediflow.doctor.DoctorService;
import com.mediflow.doctor.dto.DoctorSummaryResponse;

@RestController
@RequestMapping("/api/doctors")
public class DoctorDirectoryController {


private final DoctorService doctorService;

public DoctorDirectoryController(
    DoctorService doctorService
) {
    this.doctorService = doctorService;
}

@GetMapping
public ResponseEntity<List<DoctorSummaryResponse>>
    getAllDoctors() {

    return ResponseEntity.ok(
        doctorService.getAllDoctors()
    );
}


}
