package ru.vova.airbnb.controller.dto;

import lombok.Builder;
import lombok.Data;
import ru.vova.airbnb.entity.BookingStatus;

import java.util.Map;

@Data
@Builder
public class BookingStatisticsResponse {
    private long totalBookings;
    private Map<BookingStatus, Long> bookingsByStatus;
}
