package ru.vova.airbnb.service.scheduler.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import ru.vova.airbnb.service.BookingService;

@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
@Slf4j
public class PaymentTimeoutQuartzJob extends QuartzJobBean {

    private final BookingService bookingService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("Quartz payment timeout job started");
        bookingService.cancelExpiredPayments();
    }
}
