package com.finvault.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.finvault.common.dto.Dtos;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommonDomainTest {
    @Test
    void coversUserTransactionBudgetAlertAndGoalModels() {
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setVersion(2L);
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        user.setEmail("USER@FINVAULT.LOCAL");
        user.setFullName("User One");
        user.setPasswordHash("hash");
        user.setRole(Role.SUPER_ADMIN);
        user.setEnabled(false);
        user.setFailedAttempts(5);
        user.setLockedUntil(Instant.parse("2026-01-03T00:00:00Z"));
        user.setDeviceFingerprint("device");

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getVersion()).isEqualTo(2L);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("USER@FINVAULT.LOCAL");
        assertThat(user.getFullName()).isEqualTo("User One");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRole()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getFailedAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.getDeviceFingerprint()).isEqualTo("device");
        assertThat(Role.USER.authority()).isEqualTo("ROLE_USER");

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setUserId(1L);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setCategory("Food");
        transaction.setMerchant("Market");
        transaction.setAmount(BigDecimal.TEN);
        transaction.setOccurredAt(LocalDateTime.parse("2026-01-04T10:15:30"));
        transaction.setDescription("Groceries");
        transaction.setIdempotencyKey("idem");
        transaction.setImportJobId(3L);

        assertThat(transaction.getUserId()).isEqualTo(1L);
        assertThat(transaction.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(transaction.getCategory()).isEqualTo("Food");
        assertThat(transaction.getMerchant()).isEqualTo("Market");
        assertThat(transaction.getAmount()).isEqualByComparingTo("10");
        assertThat(transaction.getOccurredAt()).isEqualTo(LocalDateTime.parse("2026-01-04T10:15:30"));
        assertThat(transaction.getDescription()).isEqualTo("Groceries");
        assertThat(transaction.getIdempotencyKey()).isEqualTo("idem");
        assertThat(transaction.getImportJobId()).isEqualTo(3L);

        Budget budget = new Budget();
        budget.setUserId(1L);
        budget.setMonthKey("2026-01");
        budget.setCategory("Food");
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setSpentAmount(BigDecimal.valueOf(80));
        budget.setActive(false);

        assertThat(budget.getUserId()).isEqualTo(1L);
        assertThat(budget.getMonthKey()).isEqualTo("2026-01");
        assertThat(budget.getCategory()).isEqualTo("Food");
        assertThat(budget.getLimitAmount()).isEqualByComparingTo("100");
        assertThat(budget.getSpentAmount()).isEqualByComparingTo("80");
        assertThat(budget.isActive()).isFalse();

        Alert alert = new Alert();
        alert.setUserId(1L);
        alert.setBudgetId(2L);
        alert.setSeverity(AlertSeverity.CRITICAL);
        alert.setMessage("limit hit");
        alert.setReadFlag(true);

        assertThat(alert.getUserId()).isEqualTo(1L);
        assertThat(alert.getBudgetId()).isEqualTo(2L);
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(alert.getMessage()).isEqualTo("limit hit");
        assertThat(alert.isReadFlag()).isTrue();

        Goal goal = new Goal();
        goal.setUserId(1L);
        goal.setName("Trip");
        goal.setCategory("Savings");
        goal.setTargetAmount(BigDecimal.valueOf(1000));
        goal.setCurrentAmount(BigDecimal.valueOf(250));
        goal.setDeadline(LocalDate.parse("2026-12-31"));
        goal.setCompleted(true);

        assertThat(goal.getUserId()).isEqualTo(1L);
        assertThat(goal.getName()).isEqualTo("Trip");
        assertThat(goal.getCategory()).isEqualTo("Savings");
        assertThat(goal.getTargetAmount()).isEqualByComparingTo("1000");
        assertThat(goal.getCurrentAmount()).isEqualByComparingTo("250");
        assertThat(goal.getDeadline()).isEqualTo(LocalDate.parse("2026-12-31"));
        assertThat(goal.isCompleted()).isTrue();
    }

    @Test
    void coversTokenImportAuditSchedulerAndDtos() {
        UserAccount user = new UserAccount();
        user.setEmail("user@finvault.local");
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash("hash");
        token.setRevoked(true);
        token.setExpiresAt(Instant.parse("2026-01-01T00:00:00Z"));
        token.setRotatedToHash("next");
        token.setDeviceFingerprint("device");

        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getExpiresAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(token.getRotatedToHash()).isEqualTo("next");
        assertThat(token.getDeviceFingerprint()).isEqualTo("device");

        ImportJob job = new ImportJob();
        job.setUserId(1L);
        job.setFileName("transactions.csv");
        job.setTotalRows(3);
        job.setSuccessRows(2);
        job.setFailedRows(1);
        job.setStatus(JobStatus.PARTIAL);
        job.setRowErrors("[]");

        assertThat(job.getUserId()).isEqualTo(1L);
        assertThat(job.getFileName()).isEqualTo("transactions.csv");
        assertThat(job.getTotalRows()).isEqualTo(3);
        assertThat(job.getSuccessRows()).isEqualTo(2);
        assertThat(job.getFailedRows()).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo(JobStatus.PARTIAL);
        assertThat(job.getRowErrors()).isEqualTo("[]");

        AuditLog audit = new AuditLog();
        audit.setActorEmail("actor");
        audit.setAction("ACTION");
        audit.setTarget("target");
        audit.setIpAddress("127.0.0.1");
        audit.setUserAgent("JUnit");
        audit.setStatus("SUCCESS");
        audit.setDetails("details");

        assertThat(audit.getActorEmail()).isEqualTo("actor");
        assertThat(audit.getAction()).isEqualTo("ACTION");
        assertThat(audit.getTarget()).isEqualTo("target");
        assertThat(audit.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(audit.getUserAgent()).isEqualTo("JUnit");
        assertThat(audit.getStatus()).isEqualTo("SUCCESS");
        assertThat(audit.getDetails()).isEqualTo("details");

        SchedulerAudit scheduler = new SchedulerAudit();
        scheduler.setJobName("weekly");
        scheduler.setStatus(SchedulerStatus.SUCCESS);
        scheduler.setMessage("done");
        scheduler.setStartedAt(Instant.parse("2026-01-01T00:00:00Z"));
        scheduler.setCompletedAt(Instant.parse("2026-01-01T00:01:00Z"));

        assertThat(scheduler.getJobName()).isEqualTo("weekly");
        assertThat(scheduler.getStatus()).isEqualTo(SchedulerStatus.SUCCESS);
        assertThat(scheduler.getMessage()).isEqualTo("done");
        assertThat(scheduler.getStartedAt()).isNotNull();
        assertThat(scheduler.getCompletedAt()).isNotNull();

        assertThat(Dtos.ApiResponse.ok("done").success()).isTrue();
        assertThat(Dtos.ApiResponse.ok("created", 1).message()).isEqualTo("created");
        assertThat(Dtos.ApiResponse.fail("bad").success()).isFalse();
        assertThat(new Dtos.ImportRowError(1, "bad").message()).isEqualTo("bad");
        assertThat(new Dtos.BulkImportResponse(1L, 2, 1, 1, List.of()).totalRows()).isEqualTo(2);
        assertThat(new Dtos.AnalyticsOverview(List.of(), List.of(), List.of(), 0, BigDecimal.ZERO, Map.of()).categoryTotals()).isEmpty();
        assertThat(new Dtos.MetricsResponse(1, 2, 3, Map.of("status", "ok"), Map.of()).redis()).containsEntry("status", "ok");
    }
}
