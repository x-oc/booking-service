package ru.vova.airbnb.service.scheduler;

import ru.vova.airbnb.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedDelay = 3600000) // Run every hour
    public void checkPaymentTimeouts() {
        log.info("Running payment timeout check");
        bookingService.cancelExpiredPayments();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Moscow") // Run at midnight every day
    public void checkStayCompletions() {
        log.info("Running stay completion check");
        bookingService.completeStays();
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Europe/Moscow") // Run at 00:05 every day
    public void activateBookings() {
        log.info("Running booking activation for today's check-ins");
        bookingService.activateDueBookings();
    }
}