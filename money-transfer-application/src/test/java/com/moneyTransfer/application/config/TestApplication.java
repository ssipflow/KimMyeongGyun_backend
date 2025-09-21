package com.moneyTransfer.application.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.moneyTransfer.application",
    "com.moneyTransfer.persistence",
    "com.moneyTransfer.api.config"
})
@EntityScan(basePackages = "com.moneyTransfer.persistence.entity")
@EnableJpaRepositories(basePackages = "com.moneyTransfer.persistence.repository")
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}