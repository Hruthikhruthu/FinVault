package com.finvault.common.repository;

import com.finvault.common.domain.FinancialTransaction;
import com.finvault.common.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    List<FinancialTransaction> findByUserIdOrderByOccurredAtDesc(Long userId);

    Optional<FinancialTransaction> findByIdAndUserId(Long id, Long userId);

    Optional<FinancialTransaction> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Query("""
        select coalesce(sum(t.amount), 0)
        from FinancialTransaction t
        where t.userId = :userId
          and t.category = :category
          and t.type in :types
          and t.occurredAt >= :start
          and t.occurredAt < :end
        """)
    BigDecimal spendingForBudget(@Param("userId") Long userId,
                                 @Param("category") String category,
                                 @Param("types") Collection<TransactionType> types,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query(value = """
        select date_format(occurred_at, '%Y-%m') as label, coalesce(sum(amount), 0) as total
        from transactions
        where user_id = :userId and type in ('EXPENSE','SAVINGS')
        group by date_format(occurred_at, '%Y-%m')
        order by label
        """, nativeQuery = true)
    List<SpendProjection> monthlySpend(@Param("userId") Long userId);

    @Query(value = """
        select merchant as label, coalesce(sum(amount), 0) as total
        from transactions
        where user_id = :userId and type in ('EXPENSE','SAVINGS')
        group by merchant
        order by total desc
        limit 5
        """, nativeQuery = true)
    List<SpendProjection> topMerchants(@Param("userId") Long userId);

    @Query(value = """
        select category as category, dayofmonth(occurred_at) as dayNumber, coalesce(sum(amount), 0) as total
        from transactions
        where user_id = :userId and type in ('EXPENSE','SAVINGS')
        group by category, dayofmonth(occurred_at)
        order by category, dayNumber
        """, nativeQuery = true)
    List<HeatmapProjection> heatmap(@Param("userId") Long userId);

    interface SpendProjection {
        String getLabel();

        BigDecimal getTotal();
    }

    interface HeatmapProjection {
        String getCategory();

        Integer getDayNumber();

        BigDecimal getTotal();
    }
}

