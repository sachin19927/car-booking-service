CREATE INDEX idx_car_booking_vehicle_id
    ON car_booking (vehicle_id);

CREATE INDEX idx_car_booking_vehicle_rental_period
    ON car_booking (
                    vehicle_id,
                    rental_start,
                    rental_end
        );

CREATE INDEX idx_car_booking_status
    ON car_booking (booking_status);