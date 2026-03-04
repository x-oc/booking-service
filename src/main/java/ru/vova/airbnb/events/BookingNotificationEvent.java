package ru.vova.airbnb.events;

public record BookingNotificationEvent(RecipientType recipientType, Long recipientId, String message) {

    public enum RecipientType {
        GUEST,
        HOST,
        ADMIN
    }

    public static BookingNotificationEvent guest(Long guestId, String message) {
        return new BookingNotificationEvent(RecipientType.GUEST, guestId, message);
    }

    public static BookingNotificationEvent host(Long hostId, String message) {
        return new BookingNotificationEvent(RecipientType.HOST, hostId, message);
    }

    public static BookingNotificationEvent admin(String message) {
        return new BookingNotificationEvent(RecipientType.ADMIN, null, message);
    }
}

