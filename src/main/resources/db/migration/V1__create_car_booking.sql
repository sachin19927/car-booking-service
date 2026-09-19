CREATE TABLE car_booking (
                             booking_id UUID PRIMARY KEY,

                             customer_name VARCHAR(50) NOT NULL,

                             vehicle_id VARCHAR(9) NOT NULL,

                             rental_start TIMESTAMP WITH TIME ZONE NOT NULL,

                             rental_end TIMESTAMP WITH TIME ZONE NOT NULL,

                             vehicle_category VARCHAR(20) NOT NULL,

                             payment_mode VARCHAR(20) NOT NULL,

                             payment_reference VARCHAR(100),

                             booking_status VARCHAR(30) NOT NULL,

                             time_zone VARCHAR(50) NOT NULL,

                             created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             CONSTRAINT chk_rental_period
                                 CHECK (rental_end > rental_start),

                             CONSTRAINT chk_vehicle_category
                                 CHECK (
                                     vehicle_category IN (
                                                          'COMPACT',
                                                          'SEDAN',
                                                          'SUV',
                                                          'LUXURY'
                                         )
                                     ),

                             CONSTRAINT chk_payment_mode
                                 CHECK (
                                     payment_mode IN (
                                                      'CASH',
                                                      'CREDIT_CARD',
                                                      'BANK_TRANSFER'
                                         )
                                     ),

                             CONSTRAINT chk_booking_status
                                 CHECK (
                                     booking_status IN (
                                                        'PENDING_PAYMENT',
                                                        'CONFIRMED',
                                                        'CANCELLED'
                                         )
                                     )
);