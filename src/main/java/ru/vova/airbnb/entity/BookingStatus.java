package ru.vova.airbnb.entity;

public enum BookingStatus {
    CREATED,
    REJECTED,
    AWAITING_PAYMENT,
    PAID,
    CANCELLED_EXPIRED,
    CANCELLED_BY_ADMIN,
    ACTIVE,
    COMPLETED,
    FORCED_COMPLETED
}