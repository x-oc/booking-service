package ru.vova.airbnb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    @Bean
    public Clock moscowClock() {
        return Clock.system(ZoneId.of("Europe/Moscow"));
    }
}
