ALTER TABLE car_booking
    ADD COLUMN total_amount NUMERIC(19, 2),
    ADD COLUMN idempotency_key VARCHAR(100),
    ADD COLUMN request_fingerprint VARCHAR(64);

-- The assignment does not define a pricing formula. Existing rows are backfilled with
-- the same configurable-rate assumption used by the application (50/70/90/120 EUR per day).
-- One started 24-hour period counts as one rental day.
UPDATE car_booking
SET total_amount = ROUND(
        CEIL(EXTRACT(EPOCH FROM (rental_end - rental_start)) / 86400.0)
            * CASE vehicle_category
                  WHEN 'COMPACT' THEN 50.00
                  WHEN 'SEDAN' THEN 70.00
                  WHEN 'SUV' THEN 90.00
                  WHEN 'LUXURY' THEN 120.00
                  ELSE 0.00
            END,
        2)
WHERE total_amount IS NULL;

ALTER TABLE car_booking
    ALTER COLUMN total_amount SET NOT NULL;

CREATE UNIQUE INDEX ux_car_booking_idempotency_key
    ON car_booking (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_car_booking_status_deadline
    ON car_booking (booking_status, payment_deadline)
    WHERE booking_status = 'PENDING_PAYMENT';
