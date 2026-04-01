package ru.vova.airbnb.mapper;

import ru.vova.airbnb.entity.Booking;
import ru.vova.airbnb.controller.dto.BookingResponse;
import ru.vova.airbnb.controller.dto.BookingRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    BookingMapper INSTANCE = Mappers.getMapper(BookingMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "guestId", ignore = true)
    @Mapping(target = "hostId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "paymentDeadline", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "refundedAmount", ignore = true)
    @Mapping(target = "supportRequestInitiator", ignore = true)
    @Mapping(target = "supportRequestedAt", ignore = true)
    Booking toEntity(BookingRequest request);

    BookingResponse toResponse(Booking booking);
}