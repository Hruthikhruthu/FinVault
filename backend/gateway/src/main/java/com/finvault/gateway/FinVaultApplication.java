package com.finvault.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(scanBasePackages = "com.finvault")
@EntityScan(basePackages = "com.finvault.common.domain")
@EnableJpaRepositories(basePackages = "com.finvault.common.repository")
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableMethodSecurity
public class FinVaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinVaultApplication.class, args);
    }
}
