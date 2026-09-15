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
import com.portfolio.inventory.repository.ReceiptRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    private final ReceiptEventPublisher eventPublisher;
    private final Clock clock;

    public ReceiptService(ReceiptRepository receiptRepository, ItemRepository itemRepository,
                          ReceiptEventPublisher eventPublisher) {
        this(receiptRepository, itemRepository, eventPublisher, Clock.systemUTC());
    }

    ReceiptService(ReceiptRepository receiptRepository, ItemRepository itemRepository,
                   ReceiptEventPublisher eventPublisher, Clock clock) {
        this.receiptRepository = receiptRepository;
        this.itemRepository = itemRepository;
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
            receipt.addLine(item, line.quantity(), normalizeSerial(line.serialNumber()));
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
            String serial = normalizeSerial(line.serialNumber());
            if (serial != null && !serials.add(serial.toLowerCase(Locale.ROOT))) {
                throw new BusinessValidationException("Duplicate serial number: " + serial);
            }
        }
    }

    private String normalizeSerial(String serialNumber) {
        if (serialNumber == null || serialNumber.isBlank()) return null;
        return serialNumber.trim();
    }

    private ReceiptResponse toResponse(Receipt receipt) {
        List<ReceiptLineResponse> lines = receipt.getLines().stream()
                .map(line -> new ReceiptLineResponse(line.getItem().getId(), line.getItem().getSku(),
                        line.getItem().getName(), line.getQuantity(), line.getSerialNumber()))
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

