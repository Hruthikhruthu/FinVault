package com.finvault.budget;

import com.finvault.common.domain.Alert;
import com.finvault.common.domain.AlertSeverity;
import com.finvault.common.domain.Budget;
import com.finvault.common.domain.TransactionType;
import com.finvault.common.dto.Dtos.AlertResponse;
import com.finvault.common.dto.Dtos.BudgetRequest;
import com.finvault.common.dto.Dtos.BudgetResponse;
import com.finvault.common.event.TransactionCreatedEvent;
import com.finvault.common.repository.AlertRepository;
import com.finvault.common.repository.BudgetRepository;
import com.finvault.common.repository.TransactionRepository;
import com.finvault.common.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final AlertRepository alertRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailNotificationService emailNotificationService;

    public BudgetService(BudgetRepository budgetRepository,
                         AlertRepository alertRepository,
                         TransactionRepository transactionRepository,
                         UserRepository userRepository,
                         SimpMessagingTemplate messagingTemplate,
                         EmailNotificationService emailNotificationService) {
        this.budgetRepository = budgetRepository;
        this.alertRepository = alertRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public BudgetResponse createOrUpdate(Long userId, BudgetRequest request) {
        Budget budget = budgetRepository.findByUserIdAndMonthKeyAndCategoryIgnoreCase(userId, request.monthKey(), request.category())
            .orElseGet(Budget::new);
        budget.setUserId(userId);
        budget.setMonthKey(request.monthKey());
        budget.setCategory(request.category().trim());
        budget.setLimitAmount(request.limitAmount());
        budget.setActive(true);
        budget.setSpentAmount(currentSpend(userId, request.category(), request.monthKey()));
        Budget saved = budgetRepository.save(budget);
        evaluate(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId) {
        return budgetRepository.findByUserIdOrderByMonthKeyDescCategoryAsc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> alerts(Long userId) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AlertResponse markRead(Long userId, Long alertId) {
        Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setReadFlag(true);
        return toResponse(alertRepository.save(alert));
    }

    @EventListener
    @Transactional
    public void onTransactionCreated(TransactionCreatedEvent event) {
        if (event.type() == TransactionType.INCOME || event.type() == TransactionType.TRANSFER) {
            return;
        }
        String monthKey = YearMonth.from(event.occurredAt()).toString();
        budgetRepository.findByUserIdAndMonthKeyAndActiveTrue(event.userId(), monthKey).stream()
            .filter(budget -> budget.getCategory().equalsIgnoreCase(event.category()))
            .forEach(budget -> {
                budget.setSpentAmount(budget.getSpentAmount().add(event.amount()));
                Budget saved = budgetRepository.save(budget);
                evaluate(saved);
            });
    }

    private BigDecimal currentSpend(Long userId, String category, String monthKey) {
        YearMonth month = YearMonth.parse(monthKey);
        return transactionRepository.spendingForBudget(
            userId,
            category,
            List.of(TransactionType.EXPENSE, TransactionType.SAVINGS),
            month.atDay(1).atStartOfDay(),
            month.plusMonths(1).atDay(1).atStartOfDay()
        );
    }

    private void evaluate(Budget budget) {
        if (budget.getLimitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal percent = budget.getSpentAmount()
            .multiply(BigDecimal.valueOf(100))
            .divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP);
        AlertSeverity severity = null;
        if (percent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            severity = AlertSeverity.CRITICAL;
        } else if (percent.compareTo(BigDecimal.valueOf(80)) >= 0) {
            severity = AlertSeverity.WARNING;
        }
        if (severity == null) {
            return;
        }
        AlertSeverity alertSeverity = severity;
        Alert alert = new Alert();
        alert.setUserId(budget.getUserId());
        alert.setBudgetId(budget.getId());
        alert.setSeverity(alertSeverity);
        alert.setMessage(budget.getCategory() + " budget is at " + percent + "% for " + budget.getMonthKey());
        Alert saved = alertRepository.save(alert);
        AlertResponse response = toResponse(saved);
        messagingTemplate.convertAndSend("/topic/users/" + budget.getUserId() + "/alerts", response);
        userRepository.findById(budget.getUserId()).ifPresent(user ->
            emailNotificationService.sendBudgetAlert(user.getEmail(), "FinVault budget " + alertSeverity, alert.getMessage()));
    }

    private BudgetResponse toResponse(Budget budget) {
        double percent = budget.getLimitAmount().compareTo(BigDecimal.ZERO) == 0
            ? 0
            : budget.getSpentAmount().multiply(BigDecimal.valueOf(100))
                .divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP)
                .doubleValue();
        return new BudgetResponse(
            budget.getId(),
            budget.getMonthKey(),
            budget.getCategory(),
            budget.getLimitAmount(),
            budget.getSpentAmount(),
            percent,
            budget.isActive()
        );
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getSeverity(), alert.getMessage(), alert.isReadFlag(), alert.getCreatedAt());
    }
}
