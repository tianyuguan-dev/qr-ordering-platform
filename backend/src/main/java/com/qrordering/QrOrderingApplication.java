package com.qrordering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * QR Ordering Platform - Main Application Entry Point
 *
 * A multi-tenant SaaS platform for restaurant QR code ordering
 */
@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks for Outbox Processor
public class QrOrderingApplication {

    public static void main(String[] args) {
        SpringApplication.run(QrOrderingApplication.class, args);
    }

}
