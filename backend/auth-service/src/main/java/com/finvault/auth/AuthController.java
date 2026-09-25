package com.finvault.auth;

import com.finvault.common.dto.Dtos.ApiResponse;
import com.finvault.common.dto.Dtos.AuthResponse;
import com.finvault.common.dto.Dtos.LoginRequest;
import com.finvault.common.dto.Dtos.RefreshRequest;
import com.finvault.common.dto.Dtos.RegisterRequest;
import com.finvault.common.dto.Dtos.UserProfile;
import com.finvault.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(AuthService authService, JwtService jwtService, TokenBlacklistService tokenBlacklistService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.register(request, fingerprint(servletRequest, null), ip(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.login(request.email(), request.password(), fingerprint(servletRequest, request.deviceFingerprint()), ip(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.refresh(request.refreshToken(), fingerprint(servletRequest, null)));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshRequest request,
                                    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            JwtService.JwtClaims claims = jwtService.validate(authorization.substring(7));
            tokenBlacklistService.blacklist(claims.jwtId(), claims.expiresAt());
        }
        if (request != null) {
            authService.revokeRefresh(request.refreshToken());
        }
        return ApiResponse.ok("logged out", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserProfile> me() {
        return ApiResponse.ok(authService.profile(SecurityUtils.currentUserId()));
    }

    private String fingerprint(HttpServletRequest request, String candidate) {
        String header = request.getHeader("X-Device-Fingerprint");
        if (header != null && !header.isBlank()) {
            return header;
        }
        if (candidate != null && !candidate.isBlank()) {
            return candidate;
        }
        return Integer.toHexString((userAgent(request) + "|" + ip(request)).hashCode());
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

    private String userAgent(HttpServletRequest request) {
        String agent = request.getHeader("User-Agent");
        return agent == null ? "unknown" : agent;
    }
}

