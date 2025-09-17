package com.moneyTransfer.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.moneyTransfer.persistence.repository")
@EntityScan(basePackages = "com.moneyTransfer.persistence.entity")
@EnableTransactionManagement
public class JpaConfig {
}