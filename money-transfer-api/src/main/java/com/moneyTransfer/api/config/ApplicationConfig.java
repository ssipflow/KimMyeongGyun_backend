package com.moneyTransfer.api.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
    "com.moneyTransfer.api",           // API 계층
    "com.moneyTransfer.application",    // Application 계층
    "com.moneyTransfer.persistence"     // Persistence 계층
})
public class ApplicationConfig {
}