package com.moneyTransfer.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.moneyTransfer")
public class MoneyTransferApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoneyTransferApplication.class, args);
    }
}