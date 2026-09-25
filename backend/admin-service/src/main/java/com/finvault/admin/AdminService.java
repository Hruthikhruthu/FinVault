package com.finvault.admin;

import com.finvault.common.domain.AuditLog;
import com.finvault.common.domain.UserAccount;
import com.finvault.common.dto.Dtos.AdminUserResponse;
import com.finvault.common.dto.Dtos.AuditResponse;
import com.finvault.common.dto.Dtos.MetricsResponse;
import com.finvault.common.repository.AlertRepository;
import com.finvault.common.repository.AuditLogRepository;
import com.finvault.common.repository.UserRepository;
import com.finvault.common.security.PasswordPolicy;
import com.finvault.common.service.AuditLogger;
import com.finvault.common.service.WebSocketMetrics;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AlertRepository alertRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;
    private final ObjectProvider<WebSocketMetrics> webSocketMetrics;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactory;
    private final DataSource dataSource;

    public AdminService(UserRepository userRepository,
                        AuditLogRepository auditLogRepository,
                        AlertRepository alertRepository,
                        PasswordEncoder passwordEncoder,
                        AuditLogger auditLogger,
                        ObjectProvider<WebSocketMetrics> webSocketMetrics,
                        ObjectProvider<RedisConnectionFactory> redisConnectionFactory,
                        DataSource dataSource) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.alertRepository = alertRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
        this.webSocketMetrics = webSocketMetrics;
        this.redisConnectionFactory = redisConnectionFactory;
        this.dataSource = dataSource;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> users() {
        return userRepository.findAll().stream().map(this::toUser).toList();
    }

    @Transactional
    public AdminUserResponse resetPassword(Long userId, String newPassword, String actor) {
        if (!PasswordPolicy.isValid(newPassword)) {
            throw new IllegalArgumentException(PasswordPolicy.message());
        }
        UserAccount user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        auditLogger.log(actor, "ADMIN_RESET_PASSWORD", user.getEmail(), "SUCCESS", "Password reset by admin", null, null);
        return toUser(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse deactivate(Long userId, String actor) {
        UserAccount user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setEnabled(false);
        auditLogger.log(actor, "ADMIN_DEACTIVATE", user.getEmail(), "SUCCESS", "Account deactivated", null, null);
        return toUser(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> auditLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::toAudit).toList();
    }

    @Transactional(readOnly = true)
    public MetricsResponse metrics() {
        long users = userRepository.count();
        long unread = userRepository.findAll().stream()
            .mapToLong(user -> alertRepository.countByUserIdAndReadFlagFalse(user.getId()))
            .sum();
        return new MetricsResponse(users, unread, webSocketMetrics.getIfAvailable(() -> () -> 0).activeSessions(), redis(), hikari());
    }

    private Map<String, Object> redis() {
        Map<String, Object> result = new LinkedHashMap<>();
        RedisConnectionFactory factory = redisConnectionFactory.getIfAvailable();
        if (factory == null) {
            result.put("status", "not configured");
            return result;
        }
        try (RedisConnection connection = factory.getConnection()) {
            Properties info = connection.serverCommands().info();
            result.put("status", "connected");
            result.put("version", info == null ? "unknown" : info.getProperty("redis_version", "unknown"));
            Long dbSize = connection.serverCommands().dbSize();
            result.put("keys", dbSize == null ? 0 : dbSize);
        } catch (RuntimeException ex) {
            result.put("status", "unavailable");
            result.put("error", ex.getMessage());
        }
        return result;
    }

    private Map<String, Object> hikari() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (dataSource instanceof HikariDataSource hikari && hikari.getHikariPoolMXBean() != null) {
            result.put("active", hikari.getHikariPoolMXBean().getActiveConnections());
            result.put("idle", hikari.getHikariPoolMXBean().getIdleConnections());
            result.put("total", hikari.getHikariPoolMXBean().getTotalConnections());
            result.put("threadsAwaiting", hikari.getHikariPoolMXBean().getThreadsAwaitingConnection());
        } else {
            result.put("status", "hikari metrics unavailable");
        }
        return result;
    }

    private AdminUserResponse toUser(UserAccount user) {
        return new AdminUserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            user.isEnabled(),
            user.getFailedAttempts(),
            user.getLockedUntil()
        );
    }

    private AuditResponse toAudit(AuditLog audit) {
        return new AuditResponse(
            audit.getId(),
            audit.getActorEmail(),
            audit.getAction(),
            audit.getTarget(),
            audit.getStatus(),
            audit.getDetails(),
            audit.getCreatedAt()
        );
    }
}
