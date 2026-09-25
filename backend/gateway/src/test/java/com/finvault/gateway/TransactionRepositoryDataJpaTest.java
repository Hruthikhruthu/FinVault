package com.finvault.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.finvault.common.domain.FinancialTransaction;
import com.finvault.common.domain.TransactionType;
import com.finvault.common.domain.UserAccount;
import com.finvault.common.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryDataJpaTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void findsUserTransactionsAndBudgetSpend() {
        UserAccount user = new UserAccount();
        user.setEmail("data@finvault.local");
        user.setFullName("Data User");
        user.setPasswordHash("hash");
        entityManager.persistAndFlush(user);

        FinancialTransaction expense = transaction(user.getId(), TransactionType.EXPENSE, "Food", "Market", "42.50");
        FinancialTransaction income = transaction(user.getId(), TransactionType.INCOME, "Salary", "Employer", "1000.00");
        entityManager.persist(expense);
        entityManager.persist(income);
        entityManager.flush();

        assertThat(transactionRepository.findByUserIdOrderByOccurredAtDesc(user.getId())).hasSize(2);
        assertThat(transactionRepository.findByIdAndUserId(expense.getId(), user.getId())).isPresent();
        assertThat(transactionRepository.spendingForBudget(
            user.getId(),
            "Food",
            List.of(TransactionType.EXPENSE, TransactionType.SAVINGS),
            LocalDateTime.parse("2026-01-01T00:00:00"),
            LocalDateTime.parse("2026-02-01T00:00:00")
        )).isEqualByComparingTo("42.50");
    }

    private FinancialTransaction transaction(Long userId, TransactionType type, String category, String merchant, String amount) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setUserId(userId);
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setMerchant(merchant);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setOccurredAt(LocalDateTime.parse("2026-01-10T09:30:00"));
        transaction.setDescription("test");
        return transaction;
    }
}
