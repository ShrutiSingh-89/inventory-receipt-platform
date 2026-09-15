package com.portfolio.inventory.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReceiptAuditConsumer {
    private static final Logger log = LoggerFactory.getLogger(ReceiptAuditConsumer.class);

    @KafkaListener(topics = "${app.kafka.receipt-topic}")
    public void consume(ReceiptCreatedEvent event) {
        log.info("AUDIT event={} receipt={} number={} totalUnits={} occurredAt={}",
                event.eventId(), event.receiptId(), event.receiptNumber(), event.totalUnits(), event.occurredAt());
    }
}

