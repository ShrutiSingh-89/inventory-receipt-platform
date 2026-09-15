package com.portfolio.inventory.event;

import java.time.Instant;
import java.util.UUID;

public record ReceiptCreatedEvent(
        UUID eventId,
        UUID receiptId,
        String receiptNumber,
        int totalUnits,
        Instant occurredAt) {}

