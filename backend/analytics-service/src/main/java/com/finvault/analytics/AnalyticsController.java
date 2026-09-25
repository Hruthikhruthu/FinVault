package com.finvault.analytics;

import com.finvault.common.dto.Dtos.AnalyticsOverview;
import com.finvault.common.dto.Dtos.ApiResponse;
import com.finvault.common.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ApiResponse<AnalyticsOverview> overview() {
        return ApiResponse.ok(analyticsService.overview(SecurityUtils.currentUserId()));
    }
}

