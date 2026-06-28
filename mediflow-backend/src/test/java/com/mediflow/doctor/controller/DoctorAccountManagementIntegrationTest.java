package com.mediflow.doctor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.mediflow.auth.JwtService;
import com.mediflow.doctor.DoctorProfile;
import com.mediflow.doctor.DoctorProfileRepository;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DoctorAccountManagementIntegrationTest {

@Autowired
private MockMvc mockMvc;

@Autowired
private UserRepository userRepository;

@Autowired
private DoctorProfileRepository doctorProfileRepository;

@Autowired
private JwtService jwtService;

@Test
void adminCanDisableDoctorAndExistingTokenIsRejected()
    throws Exception {

    String uniqueValue = UUID.randomUUID().toString();

    User admin = createUser(
        "Account Admin",
        "account-admin-" + uniqueValue + "@example.com",
        Role.ADMIN
    );

    User doctor = createUser(
        "Doctor To Disable",
        "doctor-disable-" + uniqueValue + "@example.com",
        Role.DOCTOR
    );

    DoctorProfile doctorProfile =
        createDoctorProfile(
            doctor,
            "DISABLE-" + uniqueValue
        );

    String adminToken =
        jwtService.generateAccessToken(admin);

    String doctorToken =
        jwtService.generateAccessToken(doctor);

    mockMvc.perform(
        patch(
            "/api/admin/doctors/{doctorProfileId}/status",
            doctorProfile.getId()
        )
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + adminToken
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "enabled": false
                }
                """)
    )
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.doctorProfileId")
                .value(doctorProfile.getId())
        )
        .andExpect(
            jsonPath("$.userId")
                .value(doctor.getId())
        )
        .andExpect(
            jsonPath("$.enabled")
                .value(false)
        );

    User updatedDoctor =
        userRepository.findById(doctor.getId())
            .orElseThrow();

    assertThat(updatedDoctor.isEnabled())
        .isFalse();

    mockMvc.perform(
        get("/api/doctor/availability-slots")
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + doctorToken
            )
    )
        .andExpect(status().isUnauthorized());
}

@Test
void patientCannotDisableDoctorAccount()
    throws Exception {

    String uniqueValue = UUID.randomUUID().toString();

    User patient = createUser(
        "Unauthorized Patient",
        "unauthorized-patient-"
            + uniqueValue
            + "@example.com",
        Role.PATIENT
    );

    User doctor = createUser(
        "Protected Doctor",
        "protected-doctor-"
            + uniqueValue
            + "@example.com",
        Role.DOCTOR
    );

    DoctorProfile doctorProfile =
        createDoctorProfile(
            doctor,
            "PROTECTED-" + uniqueValue
        );

    String patientToken =
        jwtService.generateAccessToken(patient);

    mockMvc.perform(
        patch(
            "/api/admin/doctors/{doctorProfileId}/status",
            doctorProfile.getId()
        )
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + patientToken
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "enabled": false
                }
                """)
    )
        .andExpect(status().isForbidden());

    User unchangedDoctor =
        userRepository.findById(doctor.getId())
            .orElseThrow();

    assertThat(unchangedDoctor.isEnabled())
        .isTrue();
}

@Test
void missingEnabledStatusIsRejected()
    throws Exception {

    String uniqueValue = UUID.randomUUID().toString();

    User admin = createUser(
        "Validation Admin",
        "validation-admin-"
            + uniqueValue
            + "@example.com",
        Role.ADMIN
    );

    User doctor = createUser(
        "Validation Doctor",
        "validation-doctor-"
            + uniqueValue
            + "@example.com",
        Role.DOCTOR
    );

    DoctorProfile doctorProfile =
        createDoctorProfile(
            doctor,
            "VALIDATION-" + uniqueValue
        );

    String adminToken =
        jwtService.generateAccessToken(admin);

    mockMvc.perform(
        patch(
            "/api/admin/doctors/{doctorProfileId}/status",
            doctorProfile.getId()
        )
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + adminToken
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")
    )
        .andExpect(status().isBadRequest());

    User unchangedDoctor =
        userRepository.findById(doctor.getId())
            .orElseThrow();

    assertThat(unchangedDoctor.isEnabled())
        .isTrue();
}

private User createUser(
    String fullName,
    String email,
    Role role
) {
    User user = new User();

    user.setFullName(fullName);
    user.setEmail(email);
    user.setPasswordHash(
        "integration-test-password-hash"
    );
    user.setRole(role);
    user.setEnabled(true);

    return userRepository.saveAndFlush(user);
}

private DoctorProfile createDoctorProfile(
    User doctor,
    String licenseNumber
) {
    DoctorProfile doctorProfile =
        new DoctorProfile();

    doctorProfile.setUser(doctor);
    doctorProfile.setSpecialization(
        "General Medicine"
    );
    doctorProfile.setMedicalLicenseNumber(
        licenseNumber
    );
    doctorProfile.setConsultationFee(
        new BigDecimal("500.00")
    );
    doctorProfile.setHospitalName(
        "Account Test Hospital"
    );
    doctorProfile.setHospitalAddress(
        "10 Test Road, Chennai"
    );
    doctorProfile.setBio(
        "Doctor created for account management testing."
    );

    return doctorProfileRepository.saveAndFlush(
        doctorProfile
    );
}

}