package com.finvault.common.event;

import com.finvault.common.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionCreatedEvent(Long userId,
                                      Long transactionId,
                                      TransactionType type,
                                      String category,
                                      BigDecimal amount,
                                      LocalDateTime occurredAt) {
}

