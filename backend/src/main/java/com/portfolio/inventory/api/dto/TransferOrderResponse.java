package com.portfolio.inventory.api.dto;

import com.portfolio.inventory.domain.TransferOrderStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransferOrderResponse(
        UUID id,
        String orderNumber,
        Long sourceOrganizationId,
        String sourceOrganizationCode,
        String sourceOrganizationName,
        Long destinationOrganizationId,
        String destinationOrganizationCode,
        String destinationOrganizationName,
        Long itemId,
        String sku,
        String itemName,
        int quantity,
        TransferOrderStatus status,
        String requestedBy,
        LocalDate neededBy,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
