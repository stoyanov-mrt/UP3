package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class ClockConfig {

    /**
     * System clock used by all time-dependent business rules. app.clock.offset (default 0) shifts it,
     * so offer expiry and cancellation deadlines can be verified manually without waiting for days.
     */
    @Bean
    public Clock clock(@Value("${app.clock.offset:PT0S}") Duration offset) {
        return Clock.offset(Clock.systemDefaultZone(), offset);
    }
}
