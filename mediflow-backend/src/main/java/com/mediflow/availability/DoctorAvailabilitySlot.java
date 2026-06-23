package com.mediflow.availability;

import java.time.OffsetDateTime;

import com.mediflow.doctor.DoctorProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctor_availability_slots")
public class DoctorAvailabilitySlot {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(
    name = "doctor_profile_id",
    nullable = false
)
private DoctorProfile doctorProfile;

@Column(name = "start_time", nullable = false)
private OffsetDateTime startTime;

@Column(name = "end_time", nullable = false)
private OffsetDateTime endTime;

@Column(name = "created_at", nullable = false)
private OffsetDateTime createdAt;

@Column(name = "updated_at", nullable = false)
private OffsetDateTime updatedAt;

public DoctorAvailabilitySlot() {
}

@PrePersist
void beforeInsert() {
    OffsetDateTime now = OffsetDateTime.now();
    createdAt = now;
    updatedAt = now;
}

@PreUpdate
void beforeUpdate() {
    updatedAt = OffsetDateTime.now();
}

public Long getId() {
    return id;
}

public DoctorProfile getDoctorProfile() {
    return doctorProfile;
}

public void setDoctorProfile(
    DoctorProfile doctorProfile
) {
    this.doctorProfile = doctorProfile;
}

public OffsetDateTime getStartTime() {
    return startTime;
}

public void setStartTime(
    OffsetDateTime startTime
) {
    this.startTime = startTime;
}

public OffsetDateTime getEndTime() {
    return endTime;
}

public void setEndTime(
    OffsetDateTime endTime
) {
    this.endTime = endTime;
}

public OffsetDateTime getCreatedAt() {
    return createdAt;
}

public OffsetDateTime getUpdatedAt() {
    return updatedAt;
}


}
