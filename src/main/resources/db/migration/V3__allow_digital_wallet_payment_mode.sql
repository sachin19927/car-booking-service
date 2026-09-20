ALTER TABLE car_booking
DROP CONSTRAINT chk_payment_mode;

ALTER TABLE car_booking
    ADD CONSTRAINT chk_payment_mode
        CHECK (
            payment_mode IN (
                             'CASH',
                             'CREDIT_CARD',
                             'BANK_TRANSFER',
                             'DIGITAL_WALLET'
                )
            );