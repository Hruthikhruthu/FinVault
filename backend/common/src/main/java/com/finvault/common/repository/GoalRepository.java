package com.finvault.common.repository;

import com.finvault.common.domain.Goal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserIdOrderByDeadlineAsc(Long userId);

    List<Goal> findByUserIdAndCompletedFalseOrderByDeadlineAsc(Long userId);
}

