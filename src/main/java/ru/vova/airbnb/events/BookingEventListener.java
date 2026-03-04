package ru.vova.airbnb.events;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.vova.airbnb.service.NotificationService;

@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingNotification(BookingNotificationEvent event) {
        switch (event.recipientType()) {
            case GUEST -> notificationService.notifyGuest(event.recipientId(), event.message());
            case HOST -> notificationService.notifyHost(event.recipientId(), event.message());
            case ADMIN -> notificationService.notifyAdmin(event.message());
        }
    }
}

