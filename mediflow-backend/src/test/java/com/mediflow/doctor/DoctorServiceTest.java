package com.mediflow.doctor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mediflow.doctor.dto.CreateDoctorRequest;
import com.mediflow.doctor.dto.CreateDoctorResponse;
import com.mediflow.doctor.dto.DoctorSummaryResponse;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DoctorService doctorService;

    @Test
    void createDoctorPersistsAndReturnsHospitalDetails() {
        CreateDoctorRequest request =
            new CreateDoctorRequest(
                " Dr Hospital Test ",
                " Doctor.Hospital@Example.com ",
                "password123",
                " Cardiology ",
                " med-123 ",
                new BigDecimal("750.00"),
                " MediFlow Hospital ",
                " 1 Health Road, Chennai ",
                " Doctor bio "
            );

        when(passwordEncoder.encode("password123"))
            .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        when(
            doctorProfileRepository.save(
                any(DoctorProfile.class)
            )
        )
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        CreateDoctorResponse response =
            doctorService.createDoctor(request);

        ArgumentCaptor<DoctorProfile> profileCaptor =
            ArgumentCaptor.forClass(
                DoctorProfile.class
            );

        verify(doctorProfileRepository)
            .save(profileCaptor.capture());

        DoctorProfile savedProfile =
            profileCaptor.getValue();

        assertThat(savedProfile.getHospitalName())
            .isEqualTo("MediFlow Hospital");

        assertThat(savedProfile.getHospitalAddress())
            .isEqualTo(
                "1 Health Road, Chennai"
            );

        assertThat(response.hospitalName())
            .isEqualTo("MediFlow Hospital");

        assertThat(response.hospitalAddress())
            .isEqualTo(
                "1 Health Road, Chennai"
            );
    }

    @Test
    void doctorDirectoryReturnsHospitalDetails() {
        User doctorUser = new User();

        doctorUser.setFullName(
            "Dr Directory Test"
        );
        doctorUser.setRole(Role.DOCTOR);
        doctorUser.setEnabled(true);

        DoctorProfile doctorProfile =
            new DoctorProfile();

        doctorProfile.setUser(doctorUser);
        doctorProfile.setSpecialization(
            "General Medicine"
        );
        doctorProfile.setConsultationFee(
            new BigDecimal("500.00")
        );
        doctorProfile.setHospitalName(
            "Directory Hospital"
        );
        doctorProfile.setHospitalAddress(
            "25 Clinic Street, Chennai"
        );
        doctorProfile.setBio(
            "Directory test doctor"
        );

        when(
            doctorProfileRepository
                .findAllByUserEnabledTrueOrderByUserFullNameAsc()
        )
            .thenReturn(List.of(doctorProfile));

        List<DoctorSummaryResponse> result =
            doctorService.getAllDoctors();

        assertThat(result).hasSize(1);

        DoctorSummaryResponse doctor =
            result.getFirst();

        assertThat(doctor.hospitalName())
            .isEqualTo("Directory Hospital");

        assertThat(doctor.hospitalAddress())
            .isEqualTo(
                "25 Clinic Street, Chennai"
            );
    }
}