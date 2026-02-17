package ru.vova.airbnb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    public void notifyHost(Long hostId, String message) {
        log.info("Notification to host {}: {}", hostId, message);
        // some actual notification logic
        // e.g., send email, push notification, etc.
    }

    public void notifyGuest(Long guestId, String message) {
        log.info("Notification to guest {}: {}", guestId, message);
        // some actual notification logic
    }
}