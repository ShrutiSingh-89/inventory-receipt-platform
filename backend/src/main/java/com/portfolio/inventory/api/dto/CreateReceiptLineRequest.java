package com.portfolio.inventory.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateReceiptLineRequest(
        @NotNull Long itemId,
        @Min(value = 1, message = "Quantity must be greater than zero")
        @Max(value = 10000, message = "Quantity cannot exceed 10,000 units") int quantity,
        @NotNull
        @Size(max = 10000, message = "A receipt line cannot contain more than 10,000 serial numbers")
        List<@Size(max = 100) String> serialNumbers) {}
