package com.portfolio.inventory.api;

import com.portfolio.inventory.api.dto.AnalyzeShipmentRequest;
import com.portfolio.inventory.api.dto.CopilotRecommendationResponse;
import com.portfolio.inventory.service.ReceivingCopilotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/copilot/receiving")
public class ReceivingCopilotController {
    private final ReceivingCopilotService receivingCopilotService;

    public ReceivingCopilotController(ReceivingCopilotService receivingCopilotService) {
        this.receivingCopilotService = receivingCopilotService;
    }

    @PostMapping("/analyze")
    public CopilotRecommendationResponse analyze(@Valid @RequestBody AnalyzeShipmentRequest request) {
        return receivingCopilotService.analyze(request);
    }
}
