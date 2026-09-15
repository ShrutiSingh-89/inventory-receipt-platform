package com.portfolio.inventory.service;

import com.portfolio.inventory.api.dto.AuditEntryResponse;
import com.portfolio.inventory.api.dto.CreateReceiptLineRequest;
import com.portfolio.inventory.api.dto.CreateReceiptRequest;
import com.portfolio.inventory.api.dto.ReceiptLineResponse;
import com.portfolio.inventory.api.dto.ReceiptResponse;
import com.portfolio.inventory.domain.Item;
import com.portfolio.inventory.domain.Receipt;
import com.portfolio.inventory.domain.ReceiptStatus;
import com.portfolio.inventory.event.ReceiptCreatedEvent;
import com.portfolio.inventory.exception.BusinessValidationException;
import com.portfolio.inventory.exception.ResourceNotFoundException;
import com.portfolio.inventory.repository.ItemRepository;
import com.portfolio.inventory.repository.ReceiptLineSerialRepository;
import com.portfolio.inventory.repository.ReceiptRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiptService {
    private static final DateTimeFormatter RECEIPT_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final ReceiptRepository receiptRepository;
    private final ItemRepository itemRepository;
    private final ReceiptLineSerialRepository serialRepository;
    private final ReceiptEventPublisher eventPublisher;
    private final Clock clock;

    public ReceiptService(ReceiptRepository receiptRepository, ItemRepository itemRepository,
                          ReceiptLineSerialRepository serialRepository, ReceiptEventPublisher eventPublisher) {
        this(receiptRepository, itemRepository, serialRepository, eventPublisher, Clock.systemUTC());
    }

    ReceiptService(ReceiptRepository receiptRepository, ItemRepository itemRepository,
                   ReceiptLineSerialRepository serialRepository, ReceiptEventPublisher eventPublisher, Clock clock) {
        this.receiptRepository = receiptRepository;
        this.itemRepository = itemRepository;
        this.serialRepository = serialRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ReceiptResponse create(CreateReceiptRequest request) {
        validateUniqueSerialNumbers(request.lines());
        Instant now = clock.instant();
        UUID receiptId = UUID.randomUUID();
        Receipt receipt = new Receipt(receiptId, "RCV-" + RECEIPT_TIME.format(now) + "-" +
                receiptId.toString().substring(0, 4).toUpperCase(Locale.ROOT),
                request.supplierName().trim(), ReceiptStatus.RECEIVED, now);

        for (CreateReceiptLineRequest line : request.lines()) {
            Item item = itemRepository.findById(line.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item " + line.itemId() + " was not found"));
            List<String> serialNumbers = normalizeSerials(line.serialNumbers());
            validateSerialPolicy(item, line.quantity(), serialNumbers);
            validateSerialsAreNew(serialNumbers);
            receipt.addLine(item, line.quantity(), serialNumbers);
        }

        Receipt saved = receiptRepository.saveAndFlush(receipt);
        int totalUnits = saved.getLines().stream().mapToInt(line -> line.getQuantity()).sum();
        eventPublisher.publish(new ReceiptCreatedEvent(UUID.randomUUID(), saved.getId(),
                saved.getReceiptNumber(), totalUnits, now));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReceiptResponse get(UUID id) {
        Receipt receipt = receiptRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt " + id + " was not found"));
        return toResponse(receipt);
    }

    private void validateUniqueSerialNumbers(List<CreateReceiptLineRequest> lines) {
        Set<String> serials = new HashSet<>();
        for (CreateReceiptLineRequest line : lines) {
            for (String serial : normalizeSerials(line.serialNumbers())) {
                if (!serials.add(serial.toLowerCase(Locale.ROOT))) {
                    throw new BusinessValidationException("Duplicate serial number: " + serial);
                }
            }
        }
    }

    private List<String> normalizeSerials(List<String> serialNumbers) {
        if (serialNumbers == null) return List.of();
        List<String> normalized = new ArrayList<>();
        for (String serialNumber : serialNumbers) {
            if (serialNumber != null && !serialNumber.isBlank()) normalized.add(serialNumber.trim());
        }
        return normalized;
    }

    private void validateSerialPolicy(Item item, int quantity, List<String> serialNumbers) {
        if (item.isSerialControlled() && serialNumbers.size() != quantity) {
            throw new BusinessValidationException(
                    item.getSku() + " requires exactly one serial number for each received unit");
        }
        if (!item.isSerialControlled() && !serialNumbers.isEmpty()) {
            throw new BusinessValidationException(item.getSku() + " is not serial-controlled");
        }
    }

    private void validateSerialsAreNew(List<String> serialNumbers) {
        for (String serial : serialNumbers) {
            if (serialRepository.existsBySerialNumberIgnoreCase(serial)) {
                throw new BusinessValidationException("Serial number already exists: " + serial);
            }
        }
    }

    private ReceiptResponse toResponse(Receipt receipt) {
        List<ReceiptLineResponse> lines = receipt.getLines().stream()
                .map(line -> new ReceiptLineResponse(line.getItem().getId(), line.getItem().getSku(),
                        line.getItem().getName(), line.getQuantity(), line.getSerialNumbers().stream()
                                .map(serial -> serial.getSerialNumber()).toList()))
                .toList();
        int totalUnits = lines.stream().mapToInt(ReceiptLineResponse::quantity).sum();
        List<AuditEntryResponse> history = List.of(
                new AuditEntryResponse("RECEIVED", "Receipt validated and persisted", receipt.getCreatedAt()),
                new AuditEntryResponse("EVENT_PUBLISHED", "ReceiptCreated published to Kafka", receipt.getCreatedAt())
        );
        return new ReceiptResponse(receipt.getId(), receipt.getReceiptNumber(), receipt.getSupplierName(),
                receipt.getStatus(), receipt.getCreatedAt(), totalUnits, lines, history);
    }
}
