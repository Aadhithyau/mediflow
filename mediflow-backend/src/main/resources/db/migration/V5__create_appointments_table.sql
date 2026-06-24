CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,

    availability_slot_id BIGINT NOT NULL,

    patient_user_id BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'BOOKED',

    consultation_fee_snapshot NUMERIC(10, 2) NOT NULL,

    completed_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointments_availability_slot
        FOREIGN KEY (availability_slot_id)
        REFERENCES doctor_availability_slots(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_appointments_patient
        FOREIGN KEY (patient_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_appointments_availability_slot
        UNIQUE (availability_slot_id),

    CONSTRAINT chk_appointments_status
        CHECK (status IN ('BOOKED', 'COMPLETED')),

    CONSTRAINT chk_appointments_fee
        CHECK (consultation_fee_snapshot >= 0),

    CONSTRAINT chk_appointments_completion_state
        CHECK (
            (
                status = 'BOOKED'
                AND completed_at IS NULL
            )
            OR
            (
                status = 'COMPLETED'
                AND completed_at IS NOT NULL
            )
        )
);

CREATE INDEX idx_appointments_patient_created
ON appointments (
    patient_user_id,
    created_at
);