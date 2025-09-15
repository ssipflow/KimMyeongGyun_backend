package com.moneyTransfer.persistence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.moneyTransfer.persistence.entity")
@EnableJpaRepositories("com.moneyTransfer.persistence.repository")
public class PersistenceTestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PersistenceTestApplication.class, args);
    }
}