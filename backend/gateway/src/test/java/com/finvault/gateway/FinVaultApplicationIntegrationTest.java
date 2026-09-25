package com.finvault.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.finvault.auth.AuthController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class FinVaultApplicationIntegrationTest {
    @Test
    void startsIntegratedApplicationContext() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(FinVaultApplication.class)
            .profiles("test")
            .properties("SERVER_PORT=0", "server.port=0")
            .run()) {

            assertThat(context.getBean(AuthController.class)).isNotNull();
            assertThat(context.containsBean("jwtAuthenticationFilter")).isTrue();
        }
    }
}
