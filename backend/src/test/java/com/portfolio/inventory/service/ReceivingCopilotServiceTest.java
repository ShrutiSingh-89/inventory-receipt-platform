package com.portfolio.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.portfolio.inventory.api.dto.AnalyzeShipmentRequest;
import com.portfolio.inventory.api.dto.CopilotRecommendationResponse;
import com.portfolio.inventory.api.dto.ShipmentLineInput;
import com.portfolio.inventory.domain.InventoryOrganization;
import com.portfolio.inventory.domain.Item;
import com.portfolio.inventory.repository.InventoryOrganizationRepository;
import com.portfolio.inventory.repository.ItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReceivingCopilotServiceTest {
    @Mock ItemRepository itemRepository;
    @Mock InventoryOrganizationRepository organizationRepository;

    private ReceivingCopilotService service;
    private InventoryOrganization organization;
    private Item scanner;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T10:30:00Z"), ZoneOffset.UTC);
        service = new ReceivingCopilotService(itemRepository, organizationRepository, clock);
        organization = new InventoryOrganization(1L, "ORG-CENTRAL", "Central Distribution Center", "Columbus");
        scanner = new Item(1L, "ITM-1001", "Industrial Barcode Scanner", "Scanning & Mobility",
                "Rugged handheld scanner with charging dock", 42, true, "SCN",
                "handheld scanner,barcode gun");
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(itemRepository.findAllByOrderByNameAsc()).thenReturn(List.of(scanner));
    }

    @Test
    void matchesDescriptionAndProposesMissingSerialWithoutWritingInventory() {
        CopilotRecommendationResponse result = service.analyze(request(true, new ShipmentLineInput(
                "LINE-1", "Rugged handheld scanner with charging dock", 3, 3,
                List.of("SUP-001", "SUP-002"))));

        assertThat(result.provider()).isEqualTo("LOCAL_EXPLAINABLE");
        assertThat(result.requiresHumanApproval()).isTrue();
        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.matchedSku()).isEqualTo("ITM-1001");
            assertThat(line.matchConfidence()).isGreaterThanOrEqualTo(90);
            assertThat(line.severity()).isEqualTo("REVIEW");
            assertThat(line.proposedSerialNumbers()).hasSize(3);
            assertThat(line.proposedSerialNumbers().get(2)).startsWith("SCN-CENTRAL-260915-");
            assertThat(line.readyForReceipt()).isTrue();
        });
    }

    @Test
    void blocksUnknownProductInsteadOfInventingAnItem() {
        CopilotRecommendationResponse result = service.analyze(request(true, new ShipmentLineInput(
                "LINE-X", "Cryogenic flux capacitor", 1, 1, List.of())));

        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.matchedItemId()).isNull();
            assertThat(line.severity()).isEqualTo("BLOCKED");
            assertThat(line.readyForReceipt()).isFalse();
        });
    }

    @Test
    void blocksDuplicateSerialsAcrossShipmentLines() {
        CopilotRecommendationResponse result = service.analyze(new AnalyzeShipmentRequest(
                "Northwind Supply", 1L, "Check duplicates", true, List.of(
                new ShipmentLineInput("LINE-1", "Industrial Barcode Scanner", 1, 1, List.of("SCN-777")),
                new ShipmentLineInput("LINE-2", "handheld scanner", 1, 1, List.of("scn-777")))));

        assertThat(result.lines().get(1).severity()).isEqualTo("BLOCKED");
        assertThat(result.lines().get(1).issues()).anyMatch(issue -> issue.contains("Duplicate"));
    }

    @Test
    void blocksMissingSerialWhenGenerationIsNotAllowed() {
        CopilotRecommendationResponse result = service.analyze(request(false, new ShipmentLineInput(
                "LINE-1", "Industrial Barcode Scanner", 2, 2, List.of("SCN-001"))));

        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.severity()).isEqualTo("BLOCKED");
            assertThat(line.proposedSerialNumbers()).containsExactly("SCN-001");
        });
    }

    @Test
    void flagsQuantityMismatchForReviewWithoutInventingABlock() {
        Item labels = new Item(2L, "ITM-1002", "Thermal Label Roll", "Packaging Supplies",
                "Weather-resistant labels", 380, false, null, "4x6 labels,shipping labels");
        when(itemRepository.findAllByOrderByNameAsc()).thenReturn(List.of(labels));

        CopilotRecommendationResponse result = service.analyze(request(true, new ShipmentLineInput(
                "LINE-2", "4x6 thermal labels", 5, 7, List.of())));

        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.matchedSku()).isEqualTo("ITM-1002");
            assertThat(line.severity()).isEqualTo("REVIEW");
            assertThat(line.readyForReceipt()).isTrue();
            assertThat(line.issues()).anyMatch(issue -> issue.contains("over-receipt"));
        });
    }

    private AnalyzeShipmentRequest request(boolean allowGeneratedSerials, ShipmentLineInput line) {
        return new AnalyzeShipmentRequest("Northwind Supply", 1L,
                "Match products and explain exceptions", allowGeneratedSerials, List.of(line));
    }
}
