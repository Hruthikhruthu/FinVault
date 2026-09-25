package com.finvault.transaction;

import com.finvault.common.dto.Dtos.ApiResponse;
import com.finvault.common.dto.Dtos.BulkImportResponse;
import com.finvault.common.dto.Dtos.TransactionRequest;
import com.finvault.common.dto.Dtos.TransactionResponse;
import com.finvault.common.security.SecurityUtils;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ApiResponse<TransactionResponse> create(@Valid @RequestBody TransactionRequest request,
                                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.ok(transactionService.create(SecurityUtils.currentUserId(), request, idempotencyKey));
    }

    @GetMapping
    public ApiResponse<List<TransactionResponse>> list() {
        return ApiResponse.ok(transactionService.list(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(transactionService.get(SecurityUtils.currentUserId(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<TransactionResponse> update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return ApiResponse.ok(transactionService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        transactionService.delete(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok("deleted", null);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ApiResponse<BulkImportResponse>> importCsv(@RequestPart("file") MultipartFile file) throws IOException {
        return transactionService.importCsvAsync(SecurityUtils.currentUserId(), file.getOriginalFilename(), file.getBytes())
            .thenApply(ApiResponse::ok);
    }
}

