package ru.vova.airbnb.controller.dto;

import lombok.Data;
import lombok.Builder;
import ru.vova.airbnb.entity.BookingStatus;
import ru.vova.airbnb.entity.SupportRequestInitiator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private Long guestId;
    private Long hostId;
    private Long propertyId;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paymentDeadline;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalAmount;
    private BigDecimal refundedAmount;
    private SupportRequestInitiator supportRequestInitiator;
    private LocalDateTime supportRequestedAt;
}