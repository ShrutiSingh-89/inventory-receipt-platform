package com.portfolio.inventory.api.dto;

import java.time.Instant;

public record AuditEntryResponse(String status, String message, Instant timestamp) {}

