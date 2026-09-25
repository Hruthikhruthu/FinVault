package com.finvault.analytics;

import com.finvault.common.domain.FinancialTransaction;
import com.finvault.common.domain.TransactionType;
import com.finvault.common.dto.Dtos.AnalyticsOverview;
import com.finvault.common.dto.Dtos.HeatmapCell;
import com.finvault.common.dto.Dtos.MerchantSpend;
import com.finvault.common.dto.Dtos.MonthlyTrend;
import com.finvault.common.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final TransactionRepository transactionRepository;

    public AnalyticsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analytics", key = "'overview:' + #root.args[0]")
    public AnalyticsOverview overview(Long userId) {
        List<MonthlyTrend> monthly = transactionRepository.monthlySpend(userId).stream()
            .map(row -> new MonthlyTrend(row.getLabel(), row.getTotal()))
            .toList();
        List<MerchantSpend> merchants = transactionRepository.topMerchants(userId).stream()
            .map(row -> new MerchantSpend(row.getLabel(), row.getTotal()))
            .toList();
        List<HeatmapCell> heatmap = transactionRepository.heatmap(userId).stream()
            .map(row -> new HeatmapCell(row.getCategory(), row.getDayNumber(), row.getTotal()))
            .toList();
        Map<String, BigDecimal> categoryTotals = transactionRepository.findByUserIdOrderByOccurredAtDesc(userId).stream()
            .filter(tx -> tx.getType() == TransactionType.EXPENSE || tx.getType() == TransactionType.SAVINGS)
            .collect(Collectors.groupingBy(
                FinancialTransaction::getCategory,
                LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO, FinancialTransaction::getAmount, BigDecimal::add)
            ));
        return new AnalyticsOverview(
            monthly,
            merchants,
            heatmap,
            monthOverMonthGrowth(monthly),
            linearRegressionPrediction(monthly),
            categoryTotals
        );
    }

    private double monthOverMonthGrowth(List<MonthlyTrend> trends) {
        if (trends.size() < 2) {
            return 0;
        }
        BigDecimal previous = trends.get(trends.size() - 2).total();
        BigDecimal current = trends.get(trends.size() - 1).total();
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? 0 : 100;
        }
        return current.subtract(previous)
            .multiply(BigDecimal.valueOf(100))
            .divide(previous, 2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private BigDecimal linearRegressionPrediction(List<MonthlyTrend> trends) {
        int n = trends.size();
        if (n == 0) {
            return BigDecimal.ZERO;
        }
        if (n == 1) {
            return trends.get(0).total();
        }
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = trends.get(i).total().doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denominator = n * sumX2 - sumX * sumX;
        double slope = denominator == 0 ? 0 : (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        double prediction = Math.max(0, intercept + slope * (n + 1));
        return BigDecimal.valueOf(prediction).setScale(2, RoundingMode.HALF_UP);
    }
}
