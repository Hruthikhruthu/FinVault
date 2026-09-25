package com.finvault.common.dto;

import com.finvault.common.domain.AlertSeverity;
import com.finvault.common.domain.Role;
import com.finvault.common.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {
    }

    public record ApiResponse<T>(boolean success, String message, T data) {
        public static <T> ApiResponse<T> ok(T data) {
            return new ApiResponse<>(true, "ok", data);
        }

        public static <T> ApiResponse<T> ok(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> fail(String message) {
            return new ApiResponse<>(false, message, null);
        }
    }

    public record RegisterRequest(@Email @NotBlank String email,
                                  @NotBlank String fullName,
                                  @NotBlank String password) {
    }

    public record LoginRequest(@Email @NotBlank String email,
                               @NotBlank String password,
                               String deviceFingerprint) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(String accessToken,
                               String refreshToken,
                               String tokenType,
                               long expiresInSeconds,
                               Role role,
                               Long userId,
                               String email) {
    }

    public record UserProfile(Long id, String email, String fullName, Role role) {
    }

    public record TransactionRequest(@NotNull TransactionType type,
                                     @NotBlank String category,
                                     @NotBlank String merchant,
                                     @NotNull @Positive BigDecimal amount,
                                     LocalDateTime occurredAt,
                                     String description) {
    }

    public record TransactionResponse(Long id,
                                      TransactionType type,
                                      String category,
                                      String merchant,
                                      BigDecimal amount,
                                      LocalDateTime occurredAt,
                                      String description,
                                      String idempotencyKey) {
    }

    public record ImportRowError(int row, String message) {
    }

    public record BulkImportResponse(Long jobId,
                                     int totalRows,
                                     int successRows,
                                     int failedRows,
                                     List<ImportRowError> rowErrors) {
    }

    public record BudgetRequest(@Pattern(regexp = "\\d{4}-\\d{2}") String monthKey,
                                @NotBlank String category,
                                @NotNull @DecimalMin("1.00") BigDecimal limitAmount) {
    }

    public record BudgetResponse(Long id,
                                 String monthKey,
                                 String category,
                                 BigDecimal limitAmount,
                                 BigDecimal spentAmount,
                                 double percentUsed,
                                 boolean active) {
    }

    public record AlertResponse(Long id,
                                AlertSeverity severity,
                                String message,
                                boolean read,
                                Instant createdAt) {
    }

    public record MonthlyTrend(String month, BigDecimal total) {
    }

    public record MerchantSpend(String merchant, BigDecimal total) {
    }

    public record HeatmapCell(String category, int day, BigDecimal total) {
    }

    public record AnalyticsOverview(List<MonthlyTrend> monthlyTrends,
                                    List<MerchantSpend> topMerchants,
                                    List<HeatmapCell> heatmap,
                                    double monthOverMonthGrowth,
                                    BigDecimal nextMonthPrediction,
                                    Map<String, BigDecimal> categoryTotals) {
    }

    public record GoalRequest(@NotBlank String name,
                              @NotBlank String category,
                              @NotNull @DecimalMin("1.00") BigDecimal targetAmount,
                              @NotNull BigDecimal currentAmount,
                              @NotNull @Future LocalDate deadline) {
    }

    public record GoalResponse(Long id,
                               String name,
                               String category,
                               BigDecimal targetAmount,
                               BigDecimal currentAmount,
                               LocalDate deadline,
                               double progressPercent,
                               BigDecimal dailySavingsRequired,
                               LocalDate predictedCompletionDate,
                               boolean completed) {
    }

    public record PasswordResetRequest(@NotBlank String newPassword) {
    }

    public record AdminUserResponse(Long id,
                                    String email,
                                    String fullName,
                                    Role role,
                                    boolean enabled,
                                    int failedAttempts,
                                    Instant lockedUntil) {
    }

    public record AuditResponse(Long id,
                                String actorEmail,
                                String action,
                                String target,
                                String status,
                                String details,
                                Instant createdAt) {
    }

    public record MetricsResponse(long users,
                                  long unreadAlerts,
                                  int activeWebSocketSessions,
                                  Map<String, Object> redis,
                                  Map<String, Object> hikari) {
    }
}
