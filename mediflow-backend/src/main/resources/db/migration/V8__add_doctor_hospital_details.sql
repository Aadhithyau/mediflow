ALTER TABLE doctor_profiles
ADD COLUMN hospital_name VARCHAR(150),
ADD COLUMN hospital_address VARCHAR(500);

ALTER TABLE doctor_profiles
ADD CONSTRAINT chk_doctor_profiles_hospital_name_not_blank
CHECK (
hospital_name IS NULL
OR BTRIM(hospital_name) <> ''
),
ADD CONSTRAINT chk_doctor_profiles_hospital_address_not_blank
CHECK (
hospital_address IS NULL
OR BTRIM(hospital_address) <> ''
);