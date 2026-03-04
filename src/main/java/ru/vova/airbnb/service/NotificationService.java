package ru.vova.airbnb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    public void notifyHost(Long hostId, String message) {
        log.info("Notification to host {}: {}", hostId, message);
    }

    public void notifyGuest(Long guestId, String message) {
        log.info("Notification to guest {}: {}", guestId, message);
    }

    public void notifyAdmin(String message) {
        log.info("Notification to admins: {}", message);
    }
}

