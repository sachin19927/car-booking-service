CREATE TABLE bank_transfer_payment_event (
    event_id UUID PRIMARY KEY,
    payment_id VARCHAR(100) NOT NULL,
    booking_id UUID NOT NULL,
    payment_amount NUMERIC(19, 2) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uq_bank_transfer_payment_event_payment_id UNIQUE (payment_id),
    CONSTRAINT fk_bank_transfer_payment_event_booking
        FOREIGN KEY (booking_id) REFERENCES car_booking (booking_id),
    CONSTRAINT chk_bank_transfer_payment_event_amount
        CHECK (payment_amount > 0)
);

CREATE INDEX idx_bank_transfer_payment_event_booking_id
    ON bank_transfer_payment_event (booking_id, received_at);
