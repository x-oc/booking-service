package ru.vova.airbnb.config;

import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import ru.vova.airbnb.service.scheduler.job.BookingActivationQuartzJob;
import ru.vova.airbnb.service.scheduler.job.PaymentTimeoutQuartzJob;
import ru.vova.airbnb.service.scheduler.job.StayCompletionQuartzJob;

import java.util.TimeZone;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Configuration
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class SchedulerConfig {

    @Bean
    public JobDetail paymentTimeoutJobDetail() {
        return newJob(PaymentTimeoutQuartzJob.class)
                .withIdentity("payment-timeout-job")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger paymentTimeoutTrigger(
            @Qualifier("paymentTimeoutJobDetail") JobDetail jobDetail,
            @Value("${app.scheduler.payment-timeout-cron}") String cronExpression) {
        return newTrigger()
                .forJob(jobDetail)
                .withIdentity("payment-timeout-trigger")
                .withSchedule(cronSchedule(cronExpression).inTimeZone(TimeZone.getTimeZone("Europe/Moscow")))
                .build();
    }

    @Bean
    public JobDetail stayCompletionJobDetail() {
        return newJob(StayCompletionQuartzJob.class)
                .withIdentity("stay-completion-job")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger stayCompletionTrigger(
            @Qualifier("stayCompletionJobDetail") JobDetail jobDetail,
            @Value("${app.scheduler.stay-completion-cron}") String cronExpression) {
        return newTrigger()
                .forJob(jobDetail)
                .withIdentity("stay-completion-trigger")
                .withSchedule(cronSchedule(cronExpression).inTimeZone(TimeZone.getTimeZone("Europe/Moscow")))
                .build();
    }

    @Bean
    public JobDetail bookingActivationJobDetail() {
        return newJob(BookingActivationQuartzJob.class)
                .withIdentity("booking-activation-job")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger bookingActivationTrigger(
            @Qualifier("bookingActivationJobDetail") JobDetail jobDetail,
            @Value("${app.scheduler.activation-cron}") String cronExpression) {
        return newTrigger()
                .forJob(jobDetail)
                .withIdentity("booking-activation-trigger")
                .withSchedule(cronSchedule(cronExpression).inTimeZone(TimeZone.getTimeZone("Europe/Moscow")))
                .build();
    }
}