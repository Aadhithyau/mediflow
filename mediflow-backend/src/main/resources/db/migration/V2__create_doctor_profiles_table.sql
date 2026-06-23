CREATE TABLE doctor_profiles (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL UNIQUE,

    specialization VARCHAR(100) NOT NULL,

    medical_license_number VARCHAR(100) NOT NULL UNIQUE,

    consultation_fee NUMERIC(10, 2) NOT NULL,

    bio VARCHAR(1000),

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_doctor_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_doctor_profiles_fee
        CHECK (consultation_fee >= 0)
);