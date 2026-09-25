package com.finvault.gateway;

import com.finvault.common.domain.Alert;
import com.finvault.common.domain.AlertSeverity;
import com.finvault.common.domain.AuditLog;
import com.finvault.common.domain.Budget;
import com.finvault.common.domain.FinancialTransaction;
import com.finvault.common.domain.Goal;
import com.finvault.common.domain.Role;
import com.finvault.common.domain.TransactionType;
import com.finvault.common.domain.UserAccount;
import com.finvault.common.repository.AlertRepository;
import com.finvault.common.repository.AuditLogRepository;
import com.finvault.common.repository.BudgetRepository;
import com.finvault.common.repository.GoalRepository;
import com.finvault.common.repository.TransactionRepository;
import com.finvault.common.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {
    static final String DEMO_ADMIN_EMAIL = "admin@finvault.local";
    static final String DEMO_ADMIN_PASSWORD = "FinVault#2026";

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final AlertRepository alertRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public DemoDataInitializer(UserRepository userRepository,
                               TransactionRepository transactionRepository,
                               BudgetRepository budgetRepository,
                               GoalRepository goalRepository,
                               AlertRepository alertRepository,
                               AuditLogRepository auditLogRepository,
                               PasswordEncoder passwordEncoder,
                               TransactionTemplate transactionTemplate) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.alertRepository = alertRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> seed());
    }

    private void seed() {
        UserAccount admin = userRepository.findByEmailIgnoreCase(DEMO_ADMIN_EMAIL)
            .orElseGet(UserAccount::new);
        admin.setEmail(DEMO_ADMIN_EMAIL);
        admin.setFullName("FinVault Admin");
        admin.setPasswordHash(passwordEncoder.encode(DEMO_ADMIN_PASSWORD));
        admin.setRole(Role.SUPER_ADMIN);
        admin.setEnabled(true);
        admin.setFailedAttempts(0);
        admin.setLockedUntil(null);
        admin.setDeviceFingerprint("demo-browser");
        admin = userRepository.save(admin);

        seedTransactions(admin.getId());
        Budget groceries = seedBudget(admin.getId());
        seedAlert(admin.getId(), groceries.getId());
        seedGoal(admin.getId());
        seedAudit();
    }

    private void seedTransactions(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        saveTransaction(userId, "demo-admin-salary", TransactionType.INCOME, "Salary", "Acme Payroll",
            "5800.00", now.minusDays(18), "Monthly salary deposit");
        saveTransaction(userId, "demo-admin-groceries", TransactionType.EXPENSE, "Groceries", "Fresh Market",
            "420.75", now.minusDays(4), "Weekly groceries");
        saveTransaction(userId, "demo-admin-rent", TransactionType.EXPENSE, "Housing", "City Apartments",
            "1800.00", now.minusDays(12), "Monthly rent");
        saveTransaction(userId, "demo-admin-savings", TransactionType.SAVINGS, "Emergency Fund", "FinVault Savings",
            "650.00", now.minusDays(2), "Automated savings transfer");
        saveTransaction(userId, "demo-admin-transport", TransactionType.EXPENSE, "Transport", "Metro Card",
            "86.40", now.minusDays(7), "Transit reload");
        saveTransaction(userId, "demo-admin-previous-groceries", TransactionType.EXPENSE, "Groceries", "Fresh Market",
            "365.10", now.minusMonths(1).minusDays(3), "Previous month grocery run");
    }

    private void saveTransaction(Long userId,
                                 String idempotencyKey,
                                 TransactionType type,
                                 String category,
                                 String merchant,
                                 String amount,
                                 LocalDateTime occurredAt,
                                 String description) {
        if (transactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).isPresent()) {
            return;
        }
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setUserId(userId);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setMerchant(merchant);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setOccurredAt(occurredAt);
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }

    private Budget seedBudget(Long userId) {
        String monthKey = YearMonth.now().toString();
        Budget budget = budgetRepository.findByUserIdAndMonthKeyAndCategoryIgnoreCase(userId, monthKey, "Groceries")
            .orElseGet(Budget::new);
        budget.setUserId(userId);
        budget.setMonthKey(monthKey);
        budget.setCategory("Groceries");
        budget.setLimitAmount(new BigDecimal("350.00"));
        budget.setSpentAmount(new BigDecimal("420.75"));
        budget.setActive(true);
        return budgetRepository.save(budget);
    }

    private void seedAlert(Long userId, Long budgetId) {
        boolean exists = alertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .anyMatch(alert -> "Groceries budget is at 120.21% for demo data".equals(alert.getMessage()));
        if (exists) {
            return;
        }
        Alert alert = new Alert();
        alert.setUserId(userId);
        alert.setBudgetId(budgetId);
        alert.setSeverity(AlertSeverity.CRITICAL);
        alert.setMessage("Groceries budget is at 120.21% for demo data");
        alertRepository.save(alert);
    }

    private void seedGoal(Long userId) {
        boolean exists = goalRepository.findByUserIdOrderByDeadlineAsc(userId).stream()
            .anyMatch(goal -> "Emergency Fund".equalsIgnoreCase(goal.getName()));
        if (exists) {
            return;
        }
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setName("Emergency Fund");
        goal.setCategory("Emergency Fund");
        goal.setTargetAmount(new BigDecimal("5000.00"));
        goal.setCurrentAmount(new BigDecimal("2150.00"));
        goal.setDeadline(LocalDate.now().plusMonths(8));
        goal.setCompleted(false);
        goalRepository.save(goal);
    }

    private void seedAudit() {
        boolean exists = auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
            .anyMatch(audit -> DEMO_ADMIN_EMAIL.equalsIgnoreCase(audit.getActorEmail())
                && "DEMO_DATA_READY".equals(audit.getAction()));
        if (exists) {
            return;
        }
        AuditLog audit = new AuditLog();
        audit.setActorEmail(DEMO_ADMIN_EMAIL);
        audit.setAction("DEMO_DATA_READY");
        audit.setTarget("local-stack");
        audit.setStatus("SUCCESS");
        audit.setDetails("Demo admin and sample finance data are available");
        auditLogRepository.save(audit);
    }
}
