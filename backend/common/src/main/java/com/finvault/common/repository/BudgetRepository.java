package com.finvault.common.repository;

import com.finvault.common.domain.Budget;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdOrderByMonthKeyDescCategoryAsc(Long userId);

    List<Budget> findByUserIdAndMonthKeyAndActiveTrue(Long userId, String monthKey);

    Optional<Budget> findByUserIdAndMonthKeyAndCategoryIgnoreCase(Long userId, String monthKey, String category);
}

