CREATE TABLE doctor_availability_slots (
id BIGSERIAL PRIMARY KEY,

doctor_profile_id BIGINT NOT NULL,

start_time TIMESTAMP WITH TIME ZONE NOT NULL,

end_time TIMESTAMP WITH TIME ZONE NOT NULL,

created_at TIMESTAMP WITH TIME ZONE
    NOT NULL DEFAULT CURRENT_TIMESTAMP,

updated_at TIMESTAMP WITH TIME ZONE
    NOT NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_availability_slot_doctor
    FOREIGN KEY (doctor_profile_id)
    REFERENCES doctor_profiles(id)
    ON DELETE CASCADE,

CONSTRAINT chk_availability_slot_time
    CHECK (end_time > start_time),

CONSTRAINT uq_doctor_availability_slot
    UNIQUE (
        doctor_profile_id,
        start_time,
        end_time
    )

);

CREATE INDEX idx_availability_slot_doctor_start
ON doctor_availability_slots (
doctor_profile_id,
start_time
);
