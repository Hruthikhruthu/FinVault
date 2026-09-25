package com.finvault.goal;

import com.finvault.common.dto.Dtos.ApiResponse;
import com.finvault.common.dto.Dtos.GoalRequest;
import com.finvault.common.dto.Dtos.GoalResponse;
import com.finvault.common.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
public class GoalController {
    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ApiResponse<GoalResponse> create(@Valid @RequestBody GoalRequest request) {
        return ApiResponse.ok(goalService.create(SecurityUtils.currentUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<GoalResponse>> list() {
        return ApiResponse.ok(goalService.list(SecurityUtils.currentUserId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<GoalResponse> update(@PathVariable Long id, @Valid @RequestBody GoalRequest request) {
        return ApiResponse.ok(goalService.update(SecurityUtils.currentUserId(), id, request));
    }
}

