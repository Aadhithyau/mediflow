package com.mediflow.doctor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mediflow.doctor.dto.AdminDoctorResponse;
import com.mediflow.doctor.dto.UpdateDoctorAccountStatusRequest;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminDoctorServiceTest {

@Mock
private UserRepository userRepository;

@Mock
private DoctorProfileRepository doctorProfileRepository;

@Mock
private PasswordEncoder passwordEncoder;

@InjectMocks
private DoctorService doctorService;

@Test
void adminDoctorListIncludesDisabledDoctors() {
    User doctorUser = new User();

    doctorUser.setFullName(
        "Dr Disabled Test"
    );
    doctorUser.setEmail(
        "disabled.doctor@example.com"
    );
    doctorUser.setRole(Role.DOCTOR);
    doctorUser.setEnabled(false);

    DoctorProfile doctorProfile =
        new DoctorProfile();

    doctorProfile.setUser(doctorUser);
    doctorProfile.setSpecialization(
        "Dermatology"
    );
    doctorProfile.setMedicalLicenseNumber(
        "MED-DISABLED-001"
    );
    doctorProfile.setConsultationFee(
        new BigDecimal("600.00")
    );
    doctorProfile.setHospitalName(
        "Admin Test Hospital"
    );
    doctorProfile.setHospitalAddress(
        "10 Test Road, Chennai"
    );
    doctorProfile.setBio(
        "Disabled doctor account"
    );

    when(
        doctorProfileRepository
            .findAllByOrderByUserFullNameAsc()
    )
        .thenReturn(List.of(doctorProfile));

    List<AdminDoctorResponse> result =
        doctorService.getAllDoctorsForAdmin();

    assertThat(result).hasSize(1);

    AdminDoctorResponse doctor =
        result.getFirst();

    assertThat(doctor.fullName())
        .isEqualTo("Dr Disabled Test");

    assertThat(doctor.email())
        .isEqualTo(
            "disabled.doctor@example.com"
        );

    assertThat(doctor.enabled())
        .isFalse();

    assertThat(doctor.medicalLicenseNumber())
        .isEqualTo("MED-DISABLED-001");
}

@Test
void adminCanDisableDoctorAccount() {
    User doctorUser = new User();

    doctorUser.setEmail(
        "doctor@example.com"
    );
    doctorUser.setEnabled(true);

    DoctorProfile doctorProfile =
        new DoctorProfile();

    doctorProfile.setUser(doctorUser);
    doctorProfile.setSpecialization(
        "Cardiology"
    );
    doctorProfile.setMedicalLicenseNumber(
        "MED-STATUS-001"
    );
    doctorProfile.setConsultationFee(
        new BigDecimal("700.00")
    );
    doctorProfile.setHospitalName(
        "Status Test Hospital"
    );
    doctorProfile.setHospitalAddress(
        "20 Test Street, Chennai"
    );

    when(
        doctorProfileRepository.findById(1L)
    )
        .thenReturn(Optional.of(doctorProfile));

    AdminDoctorResponse response =
        doctorService.updateDoctorAccountStatus(
            1L,
            new UpdateDoctorAccountStatusRequest(
                false
            )
        );

    assertThat(doctorUser.isEnabled())
        .isFalse();

    assertThat(response.enabled())
        .isFalse();

    verify(userRepository).save(doctorUser);
}

}