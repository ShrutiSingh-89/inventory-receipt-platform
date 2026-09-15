package com.portfolio.inventory.api.dto;

import com.portfolio.inventory.domain.ReceiptStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReceiptResponse(
        UUID id,
        String receiptNumber,
        String supplierName,
        ReceiptStatus status,
        Instant createdAt,
        int totalUnits,
        List<ReceiptLineResponse> lines,
        List<AuditEntryResponse> auditHistory) {}

