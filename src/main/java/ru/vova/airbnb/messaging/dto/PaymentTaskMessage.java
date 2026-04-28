package ru.vova.airbnb.messaging.dto;

import java.time.LocalDateTime;

public record PaymentTaskMessage(
        Long bookingId,
        Long guestId,
        LocalDateTime requestedAt,
        String requestId,
        String sourceNodeId
) {
}
