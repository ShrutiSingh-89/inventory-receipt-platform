package com.portfolio.inventory.api.dto;

import java.util.List;

public record CopilotLineRecommendation(
        String lineId,
        String supplierDescription,
        Long matchedItemId,
        String matchedSku,
        String matchedItemName,
        int matchConfidence,
        int expectedQuantity,
        int receivedQuantity,
        String severity,
        List<String> issues,
        String recommendation,
        List<String> proposedSerialNumbers,
        boolean readyForReceipt) {}
