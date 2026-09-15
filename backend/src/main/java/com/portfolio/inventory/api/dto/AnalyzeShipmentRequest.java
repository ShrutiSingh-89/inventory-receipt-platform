package com.portfolio.inventory.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AnalyzeShipmentRequest(
        @NotBlank @Size(max = 160) String supplierName,
        @NotNull Long organizationId,
        @Size(max = 2000) String operatorInstructions,
        boolean allowGeneratedSerials,
        @NotEmpty List<@Valid ShipmentLineInput> lines) {}
