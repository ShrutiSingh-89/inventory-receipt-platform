package com.portfolio.inventory.api.dto;

import java.util.List;
import java.util.UUID;

public record CopilotRecommendationResponse(
        UUID analysisId,
        String provider,
        String organizationCode,
        String summary,
        boolean requiresHumanApproval,
        List<CopilotLineRecommendation> lines,
        List<String> guardrails) {}
