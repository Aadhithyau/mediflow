CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,

    appointment_id BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    amount NUMERIC(10, 2) NOT NULL,

    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    razorpay_reference_id VARCHAR(40),

    razorpay_payment_link_id VARCHAR(100),

    razorpay_payment_link_url VARCHAR(500),

    razorpay_payment_id VARCHAR(100),

    razorpay_webhook_event_id VARCHAR(100),

    failure_message VARCHAR(500),

    paid_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payments_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_payments_appointment
        UNIQUE (appointment_id),

    CONSTRAINT uq_payments_razorpay_reference
        UNIQUE (razorpay_reference_id),

    CONSTRAINT uq_payments_razorpay_link
        UNIQUE (razorpay_payment_link_id),

    CONSTRAINT uq_payments_razorpay_payment
        UNIQUE (razorpay_payment_id),

    CONSTRAINT uq_payments_webhook_event
        UNIQUE (razorpay_webhook_event_id),

    CONSTRAINT chk_payments_status
        CHECK (
            status IN (
                'NOT_REQUIRED',
                'LINK_CREATION_PENDING',
                'LINK_CREATION_FAILED',
                'PAYMENT_PENDING',
                'PAID'
            )
        ),

    CONSTRAINT chk_payments_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_payments_currency
        CHECK (
            CHAR_LENGTH(currency) = 3
            AND currency = UPPER(currency)
        ),

    CONSTRAINT chk_payments_state
        CHECK (
            (
                status = 'NOT_REQUIRED'
                AND amount = 0
                AND razorpay_reference_id IS NULL
                AND razorpay_payment_link_id IS NULL
                AND razorpay_payment_link_url IS NULL
                AND razorpay_payment_id IS NULL
                AND razorpay_webhook_event_id IS NULL
                AND failure_message IS NULL
                AND paid_at IS NULL
            )
            OR
            (
                status = 'LINK_CREATION_PENDING'
                AND amount > 0
                AND razorpay_reference_id IS NOT NULL
                AND razorpay_payment_link_id IS NULL
                AND razorpay_payment_link_url IS NULL
                AND razorpay_payment_id IS NULL
                AND razorpay_webhook_event_id IS NULL
                AND failure_message IS NULL
                AND paid_at IS NULL
            )
            OR
            (
                status = 'LINK_CREATION_FAILED'
                AND amount > 0
                AND razorpay_reference_id IS NOT NULL
                AND razorpay_payment_link_id IS NULL
                AND razorpay_payment_link_url IS NULL
                AND razorpay_payment_id IS NULL
                AND razorpay_webhook_event_id IS NULL
                AND failure_message IS NOT NULL
                AND paid_at IS NULL
            )
            OR
            (
                status = 'PAYMENT_PENDING'
                AND amount > 0
                AND razorpay_reference_id IS NOT NULL
                AND razorpay_payment_link_id IS NOT NULL
                AND razorpay_payment_link_url IS NOT NULL
                AND razorpay_payment_id IS NULL
                AND razorpay_webhook_event_id IS NULL
                AND failure_message IS NULL
                AND paid_at IS NULL
            )
            OR
            (
                status = 'PAID'
                AND amount > 0
                AND razorpay_reference_id IS NOT NULL
                AND razorpay_payment_link_id IS NOT NULL
                AND razorpay_payment_link_url IS NOT NULL
                AND razorpay_payment_id IS NOT NULL
                AND razorpay_webhook_event_id IS NOT NULL
                AND failure_message IS NULL
                AND paid_at IS NOT NULL
            )
        )
);

CREATE INDEX idx_payments_status
ON payments (status);