package com.natk.natk_antivirus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {

        RetryTemplate template = new RetryTemplate();

        // Retry policy: 5 попыток
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                5,
                Map.of(Exception.class, true) // любые ошибки HTTP / network
        );
        template.setRetryPolicy(retryPolicy);

        // Exponential Backoff + Random Jitter
        ExponentialRandomBackOffPolicy backoff = new ExponentialRandomBackOffPolicy();
        backoff.setInitialInterval(1000);   // 1s
        backoff.setMultiplier(2.0);         // *2
        backoff.setMaxInterval(15000);      // максимум 15s

        template.setBackOffPolicy(backoff);

        return template;
    }
}
