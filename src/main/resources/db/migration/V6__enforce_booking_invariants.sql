CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE car_booking
    ADD CONSTRAINT chk_total_amount_positive
        CHECK (total_amount > 0);

ALTER TABLE car_booking
    ADD CONSTRAINT chk_payment_received_non_negative
        CHECK (payment_received_amount IS NULL OR payment_received_amount >= 0);

-- A vehicle may have only one active booking for any overlapping rental interval.
-- Cancelled bookings release the vehicle.
ALTER TABLE car_booking
    ADD CONSTRAINT ex_car_booking_vehicle_rental_overlap
        EXCLUDE USING gist (
            vehicle_id WITH =,
            tstzrange(rental_start, rental_end, '[)') WITH &&
        )
        WHERE (booking_status <> 'CANCELLED');
