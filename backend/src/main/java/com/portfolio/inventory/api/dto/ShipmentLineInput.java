package com.portfolio.inventory.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ShipmentLineInput(
        @NotBlank @Size(max = 40) String lineId,
        @NotBlank @Size(max = 300) String supplierDescription,
        @Min(0) @Max(10000) int expectedQuantity,
        @Min(0) @Max(10000) int receivedQuantity,
        @NotNull @Size(max = 10000)
        List<@NotBlank @Size(max = 100) String> suppliedSerialNumbers) {}
