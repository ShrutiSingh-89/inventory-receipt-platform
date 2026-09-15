package com.portfolio.inventory.service;

import com.portfolio.inventory.event.ReceiptCreatedEvent;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReceiptEventPublisher {
    private final KafkaTemplate<String, ReceiptCreatedEvent> kafkaTemplate;
    private final String topic;

    public ReceiptEventPublisher(
            KafkaTemplate<String, ReceiptCreatedEvent> kafkaTemplate,
            @Value("${app.kafka.receipt-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(ReceiptCreatedEvent event) {
        try {
            kafkaTemplate.send(topic, event.receiptId().toString(), event).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Receipt event could not be published", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Receipt event could not be published", exception);
        }
    }
}
