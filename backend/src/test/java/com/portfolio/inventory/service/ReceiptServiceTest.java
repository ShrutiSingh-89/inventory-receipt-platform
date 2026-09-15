package com.portfolio.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio.inventory.api.dto.CreateReceiptLineRequest;
import com.portfolio.inventory.api.dto.CreateReceiptRequest;
import com.portfolio.inventory.api.dto.ReceiptResponse;
import com.portfolio.inventory.domain.Item;
import com.portfolio.inventory.domain.Receipt;
import com.portfolio.inventory.event.ReceiptCreatedEvent;
import com.portfolio.inventory.exception.BusinessValidationException;
import com.portfolio.inventory.exception.ResourceNotFoundException;
import com.portfolio.inventory.repository.ItemRepository;
import com.portfolio.inventory.repository.ReceiptLineSerialRepository;
import com.portfolio.inventory.repository.ReceiptRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {
    @Mock ReceiptRepository receiptRepository;
    @Mock ItemRepository itemRepository;
    @Mock ReceiptLineSerialRepository serialRepository;
    @Mock ReceiptEventPublisher eventPublisher;

    private ReceiptService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:30:00Z"), ZoneOffset.UTC);
        service = new ReceiptService(receiptRepository, itemRepository, serialRepository, eventPublisher, clock);
    }

    @Test
    void createsReceiptAndPublishesEvent() {
        Item scanner = new Item(1L, "ITM-1001", "Industrial Barcode Scanner",
                "Scanning & Mobility", "Rugged scanner", 42);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(scanner));
        when(receiptRepository.saveAndFlush(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReceiptResponse result = service.create(new CreateReceiptRequest("Northwind Supply", List.of(
                new CreateReceiptLineRequest(1L, 3, List.of()))));

        assertThat(result.receiptNumber()).startsWith("RCV-20260115-103000-");
        assertThat(result.totalUnits()).isEqualTo(3);
        assertThat(result.lines()).hasSize(1);
        ArgumentCaptor<ReceiptCreatedEvent> event = ArgumentCaptor.forClass(ReceiptCreatedEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().totalUnits()).isEqualTo(3);
        assertThat(event.getValue().receiptId()).isEqualTo(result.id());
    }

    @Test
    void rejectsDuplicateSerialNumbersIgnoringCaseAndWhitespace() {
        CreateReceiptRequest request = new CreateReceiptRequest("Supplier", List.of(
                new CreateReceiptLineRequest(1L, 1, List.of(" ABC-123 ")),
                new CreateReceiptLineRequest(2L, 1, List.of("abc-123"))));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Duplicate serial number");
        verify(receiptRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void rejectsUnknownItem() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateReceiptRequest("Supplier", List.of(
                new CreateReceiptLineRequest(99L, 2, List.of())))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Item 99 was not found");
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void requiresOneSerialForEachUnitOfASerialControlledItem() {
        Item scanner = new Item(1L, "ITM-1001", "Industrial Barcode Scanner",
                "Scanning & Mobility", "Rugged scanner", 42, true, "SCN", "handheld scanner");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(scanner));

        assertThatThrownBy(() -> service.create(new CreateReceiptRequest("Supplier", List.of(
                new CreateReceiptLineRequest(1L, 2, List.of("SCN-001"))))))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("exactly one serial number");
        verify(receiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsSerialThatAlreadyExistsInInventory() {
        Item scanner = new Item(1L, "ITM-1001", "Industrial Barcode Scanner",
                "Scanning & Mobility", "Rugged scanner", 42, true, "SCN", "handheld scanner");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(scanner));
        when(serialRepository.existsBySerialNumberIgnoreCase("SCN-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateReceiptRequest("Supplier", List.of(
                new CreateReceiptLineRequest(1L, 1, List.of("SCN-001"))))))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("already exists");
    }
}
