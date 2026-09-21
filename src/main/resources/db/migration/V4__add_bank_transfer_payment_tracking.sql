ALTER TABLE car_booking
    ADD COLUMN payment_deadline TIMESTAMP WITH TIME ZONE,
    ADD COLUMN payment_received_amount NUMERIC(19, 2),
    ADD COLUMN payment_received_at TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX ux_car_booking_bank_transfer_payment_reference
    ON car_booking (payment_reference)
    WHERE payment_mode in  ('BANK_TRANSFER','CREDIT_CARD')
      AND payment_reference IS NOT NULL;

CREATE INDEX idx_car_booking_payment_deadline
    ON car_booking (payment_deadline)
    WHERE booking_status = 'PENDING_PAYMENT';
