package com.motors.velocity.carbookingservice.mapper;

import com.motors.velocity.carbookingservice.dto.BookingRequest;
import com.motors.velocity.carbookingservice.dto.BookingResponse;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "rentalStart", expression = "java(request.startDate().toInstant())")
    @Mapping(target = "rentalEnd", expression = "java(request.endDate().toInstant())")
    @Mapping(target = "paymentMode", source = "paymentMethod")
    @Mapping(target = "bookingStatus", ignore = true)
    @Mapping(target = "paymentDeadline", ignore = true)
    @Mapping(target = "paymentReceivedAmount", ignore = true)
    @Mapping(target = "paymentReceivedAt", ignore = true)
    @Mapping(target = "timeZone", expression = "java(request.startDate().getZone().getId())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CarBooking toBooking(BookingRequest request);

    @Mapping(target = "status", source = "bookingStatus")
    BookingResponse toBookingResponse(CarBooking booking);
}
