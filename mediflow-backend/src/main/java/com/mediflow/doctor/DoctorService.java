package com.mediflow.doctor;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.doctor.dto.AdminDoctorResponse;
import com.mediflow.doctor.dto.CreateDoctorRequest;
import com.mediflow.doctor.dto.CreateDoctorResponse;
import com.mediflow.doctor.dto.DoctorSummaryResponse;
import com.mediflow.doctor.dto.UpdateDoctorAccountStatusRequest;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@Service
public class DoctorService {

private final UserRepository userRepository;
private final DoctorProfileRepository doctorProfileRepository;
private final PasswordEncoder passwordEncoder;

public DoctorService(
    UserRepository userRepository,
    DoctorProfileRepository doctorProfileRepository,
    PasswordEncoder passwordEncoder
) {
    this.userRepository = userRepository;
    this.doctorProfileRepository = doctorProfileRepository;
    this.passwordEncoder = passwordEncoder;
}

@Transactional
public CreateDoctorResponse createDoctor(
    CreateDoctorRequest request
) {
    String normalizedEmail = request.email()
        .trim()
        .toLowerCase(Locale.ROOT);

    String normalizedLicenseNumber =
        request.medicalLicenseNumber()
            .trim()
            .toUpperCase(Locale.ROOT);

    if (userRepository.existsByEmail(normalizedEmail)) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Email is already registered"
        );
    }

    if (doctorProfileRepository
        .existsByMedicalLicenseNumber(
            normalizedLicenseNumber
        )) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Medical license number is already registered"
        );
    }

    User user = new User();
    user.setFullName(request.fullName().trim());
    user.setEmail(normalizedEmail);
    user.setPasswordHash(
        passwordEncoder.encode(request.password())
    );
    user.setRole(Role.DOCTOR);
    user.setEnabled(true);

    User savedUser = userRepository.save(user);

    DoctorProfile doctorProfile = new DoctorProfile();
    doctorProfile.setUser(savedUser);
    doctorProfile.setSpecialization(
        request.specialization().trim()
    );
    doctorProfile.setMedicalLicenseNumber(
        normalizedLicenseNumber
    );
    doctorProfile.setConsultationFee(
        request.consultationFee()
    );
    doctorProfile.setHospitalName(
        request.hospitalName().trim()
    );
    doctorProfile.setHospitalAddress(
        request.hospitalAddress().trim()
    );

    String bio = request.bio();

    doctorProfile.setBio(
        bio == null || bio.isBlank()
            ? null
            : bio.trim()
    );

    DoctorProfile savedProfile =
        doctorProfileRepository.save(doctorProfile);

    return new CreateDoctorResponse(
        savedUser.getId(),
        savedProfile.getId(),
        savedUser.getFullName(),
        savedUser.getEmail(),
        savedUser.getRole().name(),
        savedProfile.getSpecialization(),
        savedProfile.getMedicalLicenseNumber(),
        savedProfile.getConsultationFee(),
        savedProfile.getHospitalName(),
        savedProfile.getHospitalAddress(),
        savedProfile.getBio(),
        "Doctor created successfully"
    );
}

@Transactional(readOnly = true)
public List<DoctorSummaryResponse> getAllDoctors() {
    return doctorProfileRepository
        .findAllByUserEnabledTrueOrderByUserFullNameAsc()
        .stream()
        .map(profile -> new DoctorSummaryResponse(
            profile.getId(),
            profile.getUser().getId(),
            profile.getUser().getFullName(),
            profile.getSpecialization(),
            profile.getConsultationFee(),
            profile.getHospitalName(),
            profile.getHospitalAddress(),
            profile.getBio()
        ))
        .toList();
}

@Transactional(readOnly = true)
public List<AdminDoctorResponse> getAllDoctorsForAdmin() {
    return doctorProfileRepository
        .findAllByOrderByUserFullNameAsc()
        .stream()
        .map(this::toAdminDoctorResponse)
        .toList();
}

@Transactional
public AdminDoctorResponse updateDoctorAccountStatus(
    Long doctorProfileId,
    UpdateDoctorAccountStatusRequest request
) {
    DoctorProfile doctorProfile =
        doctorProfileRepository
            .findById(doctorProfileId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Doctor not found"
                )
            );

    User doctorUser = doctorProfile.getUser();

    doctorUser.setEnabled(request.enabled());
    userRepository.save(doctorUser);

    return toAdminDoctorResponse(doctorProfile);
}

private AdminDoctorResponse toAdminDoctorResponse(
    DoctorProfile profile
) {
    return new AdminDoctorResponse(
        profile.getId(),
        profile.getUser().getId(),
        profile.getUser().getFullName(),
        profile.getUser().getEmail(),
        profile.getUser().isEnabled(),
        profile.getSpecialization(),
        profile.getMedicalLicenseNumber(),
        profile.getConsultationFee(),
        profile.getHospitalName(),
        profile.getHospitalAddress(),
        profile.getBio()
    );
}

}