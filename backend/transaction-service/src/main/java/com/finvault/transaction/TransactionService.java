package com.finvault.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finvault.common.domain.FinancialTransaction;
import com.finvault.common.domain.ImportJob;
import com.finvault.common.domain.JobStatus;
import com.finvault.common.domain.TransactionType;
import com.finvault.common.dto.Dtos.BulkImportResponse;
import com.finvault.common.dto.Dtos.ImportRowError;
import com.finvault.common.dto.Dtos.TransactionRequest;
import com.finvault.common.dto.Dtos.TransactionResponse;
import com.finvault.common.event.TransactionCreatedEvent;
import com.finvault.common.repository.ImportJobRepository;
import com.finvault.common.repository.TransactionRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final ImportJobRepository importJobRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              ImportJobRepository importJobRepository,
                              ApplicationEventPublisher eventPublisher,
                              SimpMessagingTemplate messagingTemplate,
                              ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.importJobRepository = importJobRepository;
        this.eventPublisher = eventPublisher;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @CacheEvict(cacheNames = "analytics", allEntries = true)
    public TransactionResponse create(Long userId, TransactionRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = transactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }
        FinancialTransaction transaction = fromRequest(userId, request);
        transaction.setIdempotencyKey(idempotencyKey);
        FinancialTransaction saved = transactionRepository.save(transaction);
        publish(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(Long userId) {
        return transactionRepository.findByUserIdOrderByOccurredAtDesc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long userId, Long id) {
        return transactionRepository.findByIdAndUserId(id, userId)
            .map(this::toResponse)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    @Transactional
    @CacheEvict(cacheNames = "analytics", allEntries = true)
    public TransactionResponse update(Long userId, Long id, TransactionRequest request) {
        FinancialTransaction transaction = transactionRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        transaction.setType(request.type());
        transaction.setCategory(request.category());
        transaction.setMerchant(request.merchant());
        transaction.setAmount(request.amount());
        transaction.setOccurredAt(request.occurredAt() == null ? LocalDateTime.now() : request.occurredAt());
        transaction.setDescription(request.description());
        FinancialTransaction saved = transactionRepository.save(transaction);
        publish(saved);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "analytics", allEntries = true)
    public void delete(Long userId, Long id) {
        FinancialTransaction transaction = transactionRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        transactionRepository.delete(transaction);
    }

    @Async
    @Transactional
    @CacheEvict(cacheNames = "analytics", allEntries = true)
    public CompletableFuture<BulkImportResponse> importCsvAsync(Long userId, String fileName, byte[] content) {
        ImportJob job = new ImportJob();
        job.setUserId(userId);
        job.setFileName(fileName == null ? "transactions.csv" : fileName);
        job.setStatus(JobStatus.PROCESSING);
        job = importJobRepository.saveAndFlush(job);
        List<ImportRowError> errors = new ArrayList<>();
        int total = 0;
        int success = 0;
        try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(reader);
            for (CSVRecord record : records) {
                total++;
                int row = (int) record.getRecordNumber() + 1;
                try {
                    TransactionRequest request = new TransactionRequest(
                        TransactionType.valueOf(record.get("type").toUpperCase()),
                        record.get("category"),
                        record.get("merchant"),
                        new BigDecimal(record.get("amount")),
                        parseDate(record.get("occurredAt")),
                        record.isMapped("description") ? record.get("description") : ""
                    );
                    FinancialTransaction transaction = fromRequest(userId, request);
                    transaction.setImportJobId(job.getId());
                    transaction.setIdempotencyKey("csv-" + job.getId() + "-" + row);
                    FinancialTransaction saved = transactionRepository.save(transaction);
                    publish(saved);
                    success++;
                } catch (RuntimeException ex) {
                    errors.add(new ImportRowError(row, ex.getMessage()));
                }
            }
        } catch (Exception ex) {
            errors.add(new ImportRowError(0, ex.getMessage()));
        }
        job.setTotalRows(total);
        job.setSuccessRows(success);
        job.setFailedRows(errors.size());
        job.setStatus(errors.isEmpty() ? JobStatus.COMPLETED : (success > 0 ? JobStatus.PARTIAL : JobStatus.FAILED));
        job.setRowErrors(toJson(errors));
        importJobRepository.save(job);
        BulkImportResponse response = new BulkImportResponse(job.getId(), total, success, errors.size(), errors);
        messagingTemplate.convertAndSend("/topic/users/" + userId + "/imports", response);
        return CompletableFuture.completedFuture(response);
    }

    private FinancialTransaction fromRequest(Long userId, TransactionRequest request) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setUserId(userId);
        transaction.setType(request.type());
        transaction.setCategory(request.category().trim());
        transaction.setMerchant(request.merchant().trim());
        transaction.setAmount(request.amount());
        transaction.setOccurredAt(request.occurredAt() == null ? LocalDateTime.now() : request.occurredAt());
        transaction.setDescription(request.description());
        return transaction;
    }

    private TransactionResponse toResponse(FinancialTransaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getType(),
            transaction.getCategory(),
            transaction.getMerchant(),
            transaction.getAmount(),
            transaction.getOccurredAt(),
            transaction.getDescription(),
            transaction.getIdempotencyKey()
        );
    }

    private void publish(FinancialTransaction transaction) {
        TransactionResponse response = toResponse(transaction);
        eventPublisher.publishEvent(new TransactionCreatedEvent(
            transaction.getUserId(),
            transaction.getId(),
            transaction.getType(),
            transaction.getCategory(),
            transaction.getAmount(),
            transaction.getOccurredAt()
        ));
        messagingTemplate.convertAndSend("/topic/users/" + transaction.getUserId() + "/transactions", response);
    }

    private LocalDateTime parseDate(String value) {
        return value == null || value.isBlank() ? LocalDateTime.now() : LocalDateTime.parse(value);
    }

    private String toJson(List<ImportRowError> errors) {
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}

