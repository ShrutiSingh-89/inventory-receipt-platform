package com.portfolio.inventory.api.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTransferOrderRequest(
        @NotNull Long sourceOrganizationId,
        @NotNull Long destinationOrganizationId,
        @NotNull Long itemId,
        @Min(1) @Max(10000) int quantity,
        @NotBlank @Size(max = 100) String requestedBy,
        @NotNull @FutureOrPresent LocalDate neededBy) {}
