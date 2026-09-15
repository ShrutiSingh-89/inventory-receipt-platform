package com.portfolio.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio.inventory.api.dto.CreateTransferOrderRequest;
import com.portfolio.inventory.api.dto.TransferOrderResponse;
import com.portfolio.inventory.api.dto.UpdateTransferOrderRequest;
import com.portfolio.inventory.domain.InventoryOrganization;
import com.portfolio.inventory.domain.Item;
import com.portfolio.inventory.domain.TransferOrder;
import com.portfolio.inventory.domain.TransferOrderStatus;
import com.portfolio.inventory.exception.BusinessValidationException;
import com.portfolio.inventory.exception.StaleTransferOrderException;
import com.portfolio.inventory.repository.InventoryOrganizationRepository;
import com.portfolio.inventory.repository.ItemRepository;
import com.portfolio.inventory.repository.TransferOrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferOrderServiceTest {
    @Mock TransferOrderRepository transferOrderRepository;
    @Mock InventoryOrganizationRepository organizationRepository;
    @Mock ItemRepository itemRepository;

    private TransferOrderService service;
    private InventoryOrganization central;
    private InventoryOrganization east;
    private Item scanner;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T10:30:00Z"), ZoneOffset.UTC);
        service = new TransferOrderService(transferOrderRepository, organizationRepository, itemRepository, clock);
        central = new InventoryOrganization(1L, "ORG-CENTRAL", "Central Distribution Center", "Columbus, OH");
        east = new InventoryOrganization(2L, "ORG-EAST", "East Regional Warehouse", "Allentown, PA");
        scanner = new Item(1L, "ITM-1001", "Industrial Barcode Scanner",
                "Scanning & Mobility", "Rugged scanner", 42);
    }

    @Test
    void createsTransferOrderBetweenDifferentOrganizations() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(central));
        when(organizationRepository.findById(2L)).thenReturn(Optional.of(east));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(scanner));
        when(transferOrderRepository.saveAndFlush(any(TransferOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferOrderResponse result = service.create(new CreateTransferOrderRequest(
                1L, 2L, 1L, 12, " Shruti Singh ", LocalDate.parse("2026-09-20")));

        assertThat(result.orderNumber()).startsWith("TO-20260915-103000-");
        assertThat(result.sourceOrganizationCode()).isEqualTo("ORG-CENTRAL");
        assertThat(result.destinationOrganizationCode()).isEqualTo("ORG-EAST");
        assertThat(result.itemName()).isEqualTo("Industrial Barcode Scanner");
        assertThat(result.quantity()).isEqualTo(12);
        assertThat(result.status()).isEqualTo(TransferOrderStatus.REQUESTED);
        assertThat(result.requestedBy()).isEqualTo("Shruti Singh");
    }

    @Test
    void rejectsTransferWithinTheSameOrganization() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(central));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(scanner));

        assertThatThrownBy(() -> service.create(new CreateTransferOrderRequest(
                1L, 1L, 1L, 5, "Shruti Singh", LocalDate.parse("2026-09-20"))))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Source and destination organizations must be different");
        verify(transferOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsQuantityAboveAvailableInventory() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(central));
        when(organizationRepository.findById(2L)).thenReturn(Optional.of(east));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(scanner));

        assertThatThrownBy(() -> service.create(new CreateTransferOrderRequest(
                1L, 2L, 1L, 43, "Shruti Singh", LocalDate.parse("2026-09-20"))))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("exceeds available inventory");
        verify(transferOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatesAnEditableTransferOrderRow() {
        UUID id = UUID.fromString("b430b623-a828-4b6a-bae1-52c588e69801");
        TransferOrder order = new TransferOrder(id, "TO-20260915-091500-B430", central, east, scanner,
                12, TransferOrderStatus.REQUESTED, "Shruti Singh", LocalDate.parse("2026-09-20"),
                Instant.parse("2026-09-15T09:15:00Z"));
        when(transferOrderRepository.findDetailedById(id)).thenReturn(Optional.of(order));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(central));
        when(organizationRepository.findById(2L)).thenReturn(Optional.of(east));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(scanner));
        when(transferOrderRepository.saveAndFlush(order)).thenReturn(order);

        TransferOrderResponse result = service.update(id, new UpdateTransferOrderRequest(
                1L, 2L, 1L, 10, "Jordan Lee", LocalDate.parse("2026-09-21"),
                TransferOrderStatus.APPROVED, 0));

        assertThat(result.quantity()).isEqualTo(10);
        assertThat(result.status()).isEqualTo(TransferOrderStatus.APPROVED);
        assertThat(result.requestedBy()).isEqualTo("Jordan Lee");
        assertThat(result.updatedAt()).isEqualTo(Instant.parse("2026-09-15T10:30:00Z"));
    }

    @Test
    void rejectsStaleTableEdit() {
        UUID id = UUID.fromString("b430b623-a828-4b6a-bae1-52c588e69801");
        TransferOrder order = new TransferOrder(id, "TO-20260915-091500-B430", central, east, scanner,
                12, TransferOrderStatus.REQUESTED, "Shruti Singh", LocalDate.parse("2026-09-20"),
                Instant.parse("2026-09-15T09:15:00Z"));
        when(transferOrderRepository.findDetailedById(id)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.update(id, new UpdateTransferOrderRequest(
                1L, 2L, 1L, 10, "Shruti Singh", LocalDate.parse("2026-09-21"),
                TransferOrderStatus.APPROVED, 4)))
                .isInstanceOf(StaleTransferOrderException.class)
                .hasMessageContaining("Refresh the table");
        verify(transferOrderRepository, never()).saveAndFlush(any());
    }
}
