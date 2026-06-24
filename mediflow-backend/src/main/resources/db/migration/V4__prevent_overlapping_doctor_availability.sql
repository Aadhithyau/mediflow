CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE doctor_availability_slots
ADD CONSTRAINT ex_doctor_availability_no_overlap
EXCLUDE USING gist (
    doctor_profile_id WITH =,
    tstzrange(start_time, end_time, '[)') WITH &&
);
