package com.portfolio.inventory.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateReceiptRequest(
        @NotBlank @Size(max = 160) String supplierName,
        @NotEmpty(message = "At least one receipt line is required") List<@Valid CreateReceiptLineRequest> lines) {}

