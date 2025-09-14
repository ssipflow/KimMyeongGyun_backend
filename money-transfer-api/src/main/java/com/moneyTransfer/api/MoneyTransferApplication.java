package com.moneyTransfer.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = "com.moneyTransfer")
@EntityScan("com.moneyTransfer.persistence.entity")
public class MoneyTransferApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoneyTransferApplication.class, args);
    }
}