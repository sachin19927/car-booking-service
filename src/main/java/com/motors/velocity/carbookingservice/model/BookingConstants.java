package com.motors.velocity.carbookingservice.model;

import java.time.Duration;

public final class BookingConstants {
    public static final String VEHICLE_ID_PATTERN = "^(VH-NL-[0-9]{3}|[0-9]{3}-NL-[A-Z]{2}|NL-[0-9]{3}-[A-Z]{2})$";
    public static final int MAX_CUSTOMER_NAME_LENGTH = 50;
    public static final int MAX_VEHICLE_ID_LENGTH = 9;
    public static final int MAX_PAYMENT_REFERENCE_LENGTH = 100;
    public static final int MAX_RENTAL_DAYS = 21;
    public static final Duration PAYMENT_DEADLINE_BEFORE_RENTAL = Duration.ofHours(48);

    public static final String BANK_TRANSFER_EVENTS_TOPIC = "bank-transfer-payment-events";
    public static final String BANK_TRANSFER_DLT_TOPIC = "bank-transfer-payment-events.DLT";
    public static final String TRANSACTION_DETAILS_FORMAT_MESSAGE =
            "transactionDetails must have format <TxnRef(12 chars)> <BookingId>";

    private BookingConstants() {}
}
