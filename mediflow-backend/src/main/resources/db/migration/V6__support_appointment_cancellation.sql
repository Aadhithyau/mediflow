ALTER TABLE appointments
DROP CONSTRAINT chk_appointments_completion_state;

ALTER TABLE appointments
DROP CONSTRAINT chk_appointments_status;

ALTER TABLE appointments
DROP CONSTRAINT uq_appointments_availability_slot;

ALTER TABLE appointments
ADD CONSTRAINT chk_appointments_status
CHECK (
    status IN (
        'BOOKED',
        'COMPLETED',
        'CANCELLED'
    )
);

ALTER TABLE appointments
ADD CONSTRAINT chk_appointments_completion_state
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
    OR
    (
        status = 'CANCELLED'
        AND completed_at IS NULL
    )
);

CREATE UNIQUE INDEX uq_appointments_non_cancelled_slot
ON appointments (availability_slot_id)
WHERE status IN ('BOOKED', 'COMPLETED');