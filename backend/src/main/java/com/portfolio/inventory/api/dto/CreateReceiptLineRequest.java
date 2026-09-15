package com.portfolio.inventory.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReceiptLineRequest(
        @NotNull Long itemId,
        @Min(value = 1, message = "Quantity must be greater than zero")
        @Max(value = 10000, message = "Quantity cannot exceed 10,000 units") int quantity,
        @Size(max = 100) String serialNumber) {}
