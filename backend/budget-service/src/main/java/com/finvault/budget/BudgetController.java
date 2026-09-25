package com.finvault.budget;

import com.finvault.common.dto.Dtos.AlertResponse;
import com.finvault.common.dto.Dtos.ApiResponse;
import com.finvault.common.dto.Dtos.BudgetRequest;
import com.finvault.common.dto.Dtos.BudgetResponse;
import com.finvault.common.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ApiResponse<BudgetResponse> create(@Valid @RequestBody BudgetRequest request) {
        return ApiResponse.ok(budgetService.createOrUpdate(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<BudgetResponse>> list() {
        return ApiResponse.ok(budgetService.list(SecurityUtils.currentUserId()));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AlertResponse>> alerts() {
        return ApiResponse.ok(budgetService.alerts(SecurityUtils.currentUserId()));
    }

    @PatchMapping("/alerts/{id}/read")
    public ApiResponse<AlertResponse> markRead(@PathVariable Long id) {
        return ApiResponse.ok(budgetService.markRead(SecurityUtils.currentUserId(), id));
    }
}

