package com.finvault.auth;

import com.finvault.common.domain.RefreshToken;
import com.finvault.common.domain.Role;
import com.finvault.common.domain.UserAccount;
import com.finvault.common.dto.Dtos.AuthResponse;
import com.finvault.common.dto.Dtos.RegisterRequest;
import com.finvault.common.dto.Dtos.UserProfile;
import com.finvault.common.repository.RefreshTokenRepository;
import com.finvault.common.repository.UserRepository;
import com.finvault.common.security.PasswordPolicy;
import com.finvault.common.service.AuditLogger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogger auditLogger;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String deviceFingerprint, String ip, String userAgent) {
        if (!PasswordPolicy.isValid(request.password())) {
            throw new IllegalArgumentException(PasswordPolicy.message());
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        UserAccount user = new UserAccount();
        user.setEmail(request.email().trim().toLowerCase());
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(userRepository.count() == 0 ? Role.SUPER_ADMIN : Role.USER);
        user.setDeviceFingerprint(deviceFingerprint);
        userRepository.save(user);
        auditLogger.log(user.getEmail(), "REGISTER", user.getEmail(), "SUCCESS", "Created " + user.getRole(), ip, userAgent);
        return issueTokens(user, deviceFingerprint);
    }

    @Transactional
    public AuthResponse login(String email, String password, String deviceFingerprint, String ip, String userAgent) {
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account disabled");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new BadCredentialsException("Account locked until " + user.getLockedUntil());
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int failures = user.getFailedAttempts() + 1;
            user.setFailedAttempts(failures);
            if (failures >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));
            }
            userRepository.save(user);
            auditLogger.log(email, "LOGIN", email, "FAILED", "failedAttempts=" + failures, ip, userAgent);
            throw new BadCredentialsException("Invalid email or password");
        }
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setDeviceFingerprint(deviceFingerprint);
        userRepository.save(user);
        auditLogger.log(user.getEmail(), "LOGIN", user.getEmail(), "SUCCESS", "device=" + deviceFingerprint, ip, userAgent);
        return issueTokens(user, deviceFingerprint);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken, String deviceFingerprint) {
        String oldHash = hash(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(oldHash)
            .filter(token -> !token.isRevoked())
            .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        UserAccount user = stored.getUser();
        String newRefresh = newRefreshToken();
        stored.setRevoked(true);
        stored.setRotatedToHash(hash(newRefresh));
        refreshTokenRepository.save(stored);
        RefreshToken next = refreshEntity(user, newRefresh, deviceFingerprint);
        refreshTokenRepository.save(next);
        auditLogger.log(user.getEmail(), "REFRESH", user.getEmail(), "SUCCESS", "refresh token rotated", null, null);
        return new AuthResponse(jwtService.generateAccessToken(user), newRefresh, "Bearer", jwtService.accessTtlSeconds(), user.getRole(), user.getId(), user.getEmail());
    }

    @Transactional
    public void revokeRefresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(refreshToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    public UserProfile profile(Long id) {
        return userRepository.findById(id)
            .map(user -> new UserProfile(user.getId(), user.getEmail(), user.getFullName(), user.getRole()))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private AuthResponse issueTokens(UserAccount user, String deviceFingerprint) {
        String refresh = newRefreshToken();
        refreshTokenRepository.save(refreshEntity(user, refresh, deviceFingerprint));
        return new AuthResponse(jwtService.generateAccessToken(user), refresh, "Bearer", jwtService.accessTtlSeconds(), user.getRole(), user.getId(), user.getEmail());
    }

    private RefreshToken refreshEntity(UserAccount user, String token, String deviceFingerprint) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(token));
        refreshToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshToken.setDeviceFingerprint(deviceFingerprint);
        return refreshToken;
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }
}

