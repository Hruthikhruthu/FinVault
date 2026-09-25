package com.finvault.goal;

import com.finvault.common.domain.Goal;
import com.finvault.common.domain.TransactionType;
import com.finvault.common.dto.Dtos.GoalRequest;
import com.finvault.common.dto.Dtos.GoalResponse;
import com.finvault.common.event.TransactionCreatedEvent;
import com.finvault.common.repository.GoalRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {
    private final GoalRepository goalRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public GoalService(GoalRepository goalRepository, SimpMessagingTemplate messagingTemplate) {
        this.goalRepository = goalRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public GoalResponse create(Long userId, GoalRequest request) {
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setName(request.name().trim());
        goal.setCategory(request.category().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(request.currentAmount() == null ? BigDecimal.ZERO : request.currentAmount());
        goal.setDeadline(request.deadline());
        goal.setCompleted(goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0);
        return toResponse(goalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(Long userId) {
        return goalRepository.findByUserIdOrderByDeadlineAsc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public GoalResponse update(Long userId, Long id, GoalRequest request) {
        Goal goal = goalRepository.findById(id)
            .filter(candidate -> candidate.getUserId().equals(userId))
            .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        goal.setName(request.name().trim());
        goal.setCategory(request.category().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(request.currentAmount());
        goal.setDeadline(request.deadline());
        goal.setCompleted(goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0);
        Goal saved = goalRepository.save(goal);
        push(saved);
        return toResponse(saved);
    }

    @EventListener
    @Transactional
    public void autoContribute(TransactionCreatedEvent event) {
        if (event.type() != TransactionType.SAVINGS) {
            return;
        }
        List<Goal> goals = goalRepository.findByUserIdAndCompletedFalseOrderByDeadlineAsc(event.userId());
        Goal target = goals.stream()
            .filter(goal -> goal.getCategory().equalsIgnoreCase(event.category()))
            .findFirst()
            .orElseGet(() -> goals.stream().min(Comparator.comparing(Goal::getDeadline)).orElse(null));
        if (target == null) {
            return;
        }
        BigDecimal before = progress(target);
        target.setCurrentAmount(target.getCurrentAmount().add(event.amount()));
        if (target.getCurrentAmount().compareTo(target.getTargetAmount()) >= 0) {
            target.setCurrentAmount(target.getTargetAmount());
            target.setCompleted(true);
        }
        Goal saved = goalRepository.save(target);
        BigDecimal after = progress(saved);
        if (crossedMilestone(before, after) || saved.isCompleted()) {
            push(saved);
        }
    }

    private boolean crossedMilestone(BigDecimal before, BigDecimal after) {
        int beforeBucket = before.divide(BigDecimal.valueOf(25), 0, RoundingMode.DOWN).intValue();
        int afterBucket = after.divide(BigDecimal.valueOf(25), 0, RoundingMode.DOWN).intValue();
        return afterBucket > beforeBucket;
    }

    private void push(Goal goal) {
        messagingTemplate.convertAndSend("/topic/users/" + goal.getUserId() + "/goals", toResponse(goal));
    }

    private GoalResponse toResponse(Goal goal) {
        BigDecimal progress = progress(goal);
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        long daysLeft = Math.max(1, ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline()));
        BigDecimal dailyRequired = remaining.divide(BigDecimal.valueOf(daysLeft), 2, RoundingMode.HALF_UP);
        LocalDate predicted = predictedCompletion(goal, remaining);
        return new GoalResponse(
            goal.getId(),
            goal.getName(),
            goal.getCategory(),
            goal.getTargetAmount(),
            goal.getCurrentAmount(),
            goal.getDeadline(),
            progress.doubleValue(),
            dailyRequired,
            predicted,
            goal.isCompleted()
        );
    }

    private BigDecimal progress(Goal goal) {
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return goal.getCurrentAmount()
            .multiply(BigDecimal.valueOf(100))
            .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
            .min(BigDecimal.valueOf(100));
    }

    private LocalDate predictedCompletion(Goal goal, BigDecimal remaining) {
        if (goal.isCompleted()) {
            return LocalDate.now();
        }
        if (goal.getCurrentAmount().compareTo(BigDecimal.ZERO) <= 0 || goal.getCreatedAt() == null) {
            return goal.getDeadline();
        }
        LocalDate created = goal.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
        long daysSoFar = Math.max(1, ChronoUnit.DAYS.between(created, LocalDate.now()));
        BigDecimal dailyAverage = goal.getCurrentAmount().divide(BigDecimal.valueOf(daysSoFar), 2, RoundingMode.HALF_UP);
        if (dailyAverage.compareTo(BigDecimal.ZERO) <= 0) {
            return goal.getDeadline();
        }
        long daysNeeded = remaining.divide(dailyAverage, 0, RoundingMode.CEILING).longValue();
        return LocalDate.now().plusDays(daysNeeded);
    }
}

