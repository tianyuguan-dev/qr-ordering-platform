package com.qrordering;

import com.qrordering.auth.config.DemoDataProperties;
import com.qrordering.auth.config.JwtProperties;
import com.qrordering.auth.config.PlatformBootstrapProperties;
import com.qrordering.config.CustomerBaseUrlProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * QR Ordering Platform - Main Application Entry Point
 *
 * A multi-tenant SaaS platform for restaurant QR code ordering
 */
@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks for Outbox Processor
@EnableConfigurationProperties({
    CustomerBaseUrlProperties.class,
    DemoDataProperties.class,
    JwtProperties.class,
    PlatformBootstrapProperties.class
})
public class QrOrderingApplication {

    public static void main(String[] args) {
        SpringApplication.run(QrOrderingApplication.class, args);
    }

}
