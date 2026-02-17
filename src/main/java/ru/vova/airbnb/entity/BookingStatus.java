package ru.vova.airbnb.entity;

public enum BookingStatus {
    CREATED,
    REJECTED,
    AWAITING_PAYMENT,
    PAID,
    CANCELLED_EXPIRED,
    ACTIVE,
    COMPLETED,
    FORCED_COMPLETED
}