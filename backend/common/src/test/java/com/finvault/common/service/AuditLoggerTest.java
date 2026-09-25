package com.finvault.common.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.finvault.common.domain.AuditLog;
import com.finvault.common.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLoggerTest {
    @Mock
    private AuditLogRepository repository;

    @Test
    void persistsAuditEntry() {
        AuditLogger logger = new AuditLogger(repository);

        logger.log("actor", "LOGIN", "target", "SUCCESS", "details", "127.0.0.1", "JUnit");

        verify(repository).save(argThat(entry ->
            entry.getActorEmail().equals("actor")
                && entry.getAction().equals("LOGIN")
                && entry.getTarget().equals("target")
                && entry.getStatus().equals("SUCCESS")
                && entry.getDetails().equals("details")
                && entry.getIpAddress().equals("127.0.0.1")
                && entry.getUserAgent().equals("JUnit")));
    }

    @Test
    void auditFailuresDoNotBreakBusinessFlow() {
        AuditLogger logger = new AuditLogger(repository);
        doThrow(new RuntimeException("database down")).when(repository).save(argThat(AuditLog.class::isInstance));

        assertThatCode(() -> logger.log("actor", "ACTION", "target", "FAILED", "details", null, null))
            .doesNotThrowAnyException();
    }
}
