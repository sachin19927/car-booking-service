package com.motors.velocity.carbookingservice.mapper;

import com.motors.velocity.carbookingservice.dto.BookingRequest;
import com.motors.velocity.carbookingservice.dto.BookingResponse;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    CarBooking toBooking(BookingRequest request);

    BookingResponse toBookingResponse(CarBooking booking);
}
