package com.portfolio.inventory.service;

import com.portfolio.inventory.api.dto.AnalyzeShipmentRequest;
import com.portfolio.inventory.api.dto.CopilotLineRecommendation;
import com.portfolio.inventory.api.dto.CopilotRecommendationResponse;
import com.portfolio.inventory.api.dto.ShipmentLineInput;
import com.portfolio.inventory.domain.InventoryOrganization;
import com.portfolio.inventory.domain.Item;
import com.portfolio.inventory.exception.ResourceNotFoundException;
import com.portfolio.inventory.repository.InventoryOrganizationRepository;
import com.portfolio.inventory.repository.ItemRepository;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceivingCopilotService {
    private static final DateTimeFormatter SERIAL_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "for", "inch", "of", "the", "to", "with");

    private final ItemRepository itemRepository;
    private final InventoryOrganizationRepository organizationRepository;
    private final Clock clock;

    public ReceivingCopilotService(ItemRepository itemRepository,
                                   InventoryOrganizationRepository organizationRepository) {
        this(itemRepository, organizationRepository, Clock.systemUTC());
    }

    ReceivingCopilotService(ItemRepository itemRepository,
                            InventoryOrganizationRepository organizationRepository, Clock clock) {
        this.itemRepository = itemRepository;
        this.organizationRepository = organizationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CopilotRecommendationResponse analyze(AnalyzeShipmentRequest request) {
        InventoryOrganization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory organization " + request.organizationId() + " was not found"));
        List<Item> catalog = itemRepository.findAllByOrderByNameAsc();
        Set<String> seenSerials = new HashSet<>();
        List<CopilotLineRecommendation> recommendations = new ArrayList<>();

        for (ShipmentLineInput line : request.lines()) {
            recommendations.add(analyzeLine(line, organization, catalog, seenSerials,
                    request.allowGeneratedSerials()));
        }

        long blocked = recommendations.stream().filter(line -> "BLOCKED".equals(line.severity())).count();
        long review = recommendations.stream().filter(line -> "REVIEW".equals(line.severity())).count();
        String summary = "%d lines analyzed: %d ready, %d need review, %d blocked"
                .formatted(recommendations.size(), recommendations.size() - review - blocked, review, blocked);
        return new CopilotRecommendationResponse(UUID.randomUUID(), "LOCAL_EXPLAINABLE",
                organization.getCode(), summary, true, recommendations, List.of(
                "Recommendations never write inventory directly",
                "Every product match and generated serial remains editable",
                "Spring business rules revalidate the final receipt",
                "A human must approve the draft before persistence"));
    }

    private CopilotLineRecommendation analyzeLine(ShipmentLineInput line, InventoryOrganization organization,
                                                   List<Item> catalog, Set<String> seenSerials,
                                                   boolean allowGeneratedSerials) {
        Match match = bestMatch(line.supplierDescription(), catalog);
        if (match == null) {
            return new CopilotLineRecommendation(line.lineId(), line.supplierDescription(), null, null, null,
                    0, line.expectedQuantity(), line.receivedQuantity(), "BLOCKED",
                    List.of("No reliable catalog match"), "Choose an internal item before creating the receipt",
                    normalizeSerials(line.suppliedSerialNumbers()), false);
        }

        Item item = match.item();
        List<String> issues = new ArrayList<>();
        List<String> proposedSerials = new ArrayList<>(normalizeSerials(line.suppliedSerialNumbers()));
        boolean blocked = false;
        boolean review = false;

        if (line.expectedQuantity() != line.receivedQuantity()) {
            String direction = line.receivedQuantity() > line.expectedQuantity() ? "over-receipt" : "under-receipt";
            issues.add("%s: expected %d but received %d".formatted(
                    direction, line.expectedQuantity(), line.receivedQuantity()));
            review = true;
        }

        for (String serial : proposedSerials) {
            if (!seenSerials.add(serial.toLowerCase(Locale.ROOT))) {
                issues.add("Duplicate supplied serial: " + serial);
                blocked = true;
            }
        }

        if (item.isSerialControlled()) {
            if (proposedSerials.size() < line.receivedQuantity()) {
                int missing = line.receivedQuantity() - proposedSerials.size();
                issues.add("%d serial number%s missing".formatted(missing, missing == 1 ? "" : "s"));
                if (allowGeneratedSerials) {
                    proposedSerials.addAll(generateSerials(item, organization, line.lineId(),
                            proposedSerials.size(), missing, seenSerials));
                    review = true;
                } else {
                    blocked = true;
                }
            } else if (proposedSerials.size() > line.receivedQuantity()) {
                issues.add("More serial numbers were supplied than units received");
                blocked = true;
            }
        } else if (!proposedSerials.isEmpty()) {
            issues.add(item.getSku() + " is not serial-controlled");
            blocked = true;
        }

        String severity = blocked ? "BLOCKED" : review ? "REVIEW" : "READY";
        String recommendation;
        if (blocked) {
            recommendation = "Correct the blocked fields and analyze again";
        } else if (review) {
            recommendation = "Review the exception and proposed values before preparing the draft";
        } else {
            recommendation = "Matched and validated; ready for human approval";
        }
        return new CopilotLineRecommendation(line.lineId(), line.supplierDescription(), item.getId(),
                item.getSku(), item.getName(), (int) Math.round(match.confidence() * 100),
                line.expectedQuantity(), line.receivedQuantity(), severity, issues, recommendation,
                proposedSerials, !blocked);
    }

    private List<String> generateSerials(Item item, InventoryOrganization organization, String lineId,
                                         int existingCount, int count, Set<String> seenSerials) {
        String prefix = item.getSerialPrefix() == null ? "SER" : item.getSerialPrefix();
        String organizationToken = organization.getCode().replace("ORG-", "");
        String date = LocalDate.now(clock.withZone(ZoneOffset.UTC)).format(SERIAL_DATE);
        int stableOffset = Math.floorMod(lineId.toUpperCase(Locale.ROOT).hashCode(), 9000) + 1000;
        List<String> generated = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int sequence = stableOffset + existingCount + index;
            String candidate = "%s-%s-%s-%04d".formatted(prefix, organizationToken, date, sequence);
            while (!seenSerials.add(candidate.toLowerCase(Locale.ROOT))) {
                sequence++;
                candidate = "%s-%s-%s-%04d".formatted(prefix, organizationToken, date, sequence);
            }
            generated.add(candidate);
        }
        return generated;
    }

    private Match bestMatch(String supplierDescription, List<Item> catalog) {
        String normalizedDescription = normalize(supplierDescription);
        Set<String> descriptionTokens = tokens(supplierDescription);
        Match best = null;
        for (Item item : catalog) {
            String searchable = String.join(" ",
                    item.getSku(), item.getName(), item.getProductCategory(),
                    nullToEmpty(item.getDescription()), nullToEmpty(item.getSearchAliases()));
            String normalizedSearchable = normalize(searchable);
            double confidence;
            if (normalizedDescription.equals(normalize(item.getSku()))) {
                confidence = 0.99;
            } else if (normalizedDescription.equals(normalize(item.getName()))) {
                confidence = 0.97;
            } else if (normalizedSearchable.contains(normalizedDescription) && normalizedDescription.length() > 4) {
                confidence = 0.94;
            } else {
                Set<String> candidateTokens = tokens(searchable);
                long overlap = descriptionTokens.stream().filter(candidateTokens::contains).count();
                confidence = descriptionTokens.isEmpty() ? 0 : 0.35 + (0.6 * overlap / descriptionTokens.size());
            }
            if (best == null || confidence > best.confidence()) best = new Match(item, confidence);
        }
        return best != null && best.confidence() >= 0.58 ? best : null;
    }

    private Set<String> tokens(String value) {
        return Arrays.stream(normalize(value).split(" "))
                .filter(token -> token.length() > 1 && !STOP_WORDS.contains(token))
                .collect(Collectors.toSet());
    }

    private String normalize(String value) {
        return Normalizer.normalize(nullToEmpty(value), Normalizer.Form.NFKD)
                .replaceAll("[^a-zA-Z0-9]+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private List<String> normalizeSerials(List<String> serials) {
        return serials.stream().map(String::trim).filter(serial -> !serial.isBlank()).toList();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Match(Item item, double confidence) {}
}
