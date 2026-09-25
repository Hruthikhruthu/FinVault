package com.finvault.admin;

import com.finvault.common.dto.Dtos.AdminUserResponse;
import com.finvault.common.dto.Dtos.ApiResponse;
import com.finvault.common.dto.Dtos.AuditResponse;
import com.finvault.common.dto.Dtos.MetricsResponse;
import com.finvault.common.dto.Dtos.PasswordResetRequest;
import com.finvault.common.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserResponse>> users() {
        return ApiResponse.ok(adminService.users());
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<AdminUserResponse> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        return ApiResponse.ok(adminService.resetPassword(id, request.newPassword(), SecurityUtils.currentEmail()));
    }

    @PatchMapping("/users/{id}/deactivate")
    public ApiResponse<AdminUserResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok(adminService.deactivate(id, SecurityUtils.currentEmail()));
    }

    @GetMapping("/audit")
    public ApiResponse<List<AuditResponse>> audit() {
        return ApiResponse.ok(adminService.auditLogs());
    }

    @GetMapping("/metrics")
    public ApiResponse<MetricsResponse> metrics() {
        return ApiResponse.ok(adminService.metrics());
    }
}

