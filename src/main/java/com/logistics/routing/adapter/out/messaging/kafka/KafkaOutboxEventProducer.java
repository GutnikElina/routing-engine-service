package com.logistics.routing.adapter.out.messaging.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.logistics.routing.adapter.out.persistence.outbox.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class KafkaOutboxEventProducer {

    private final KafkaTemplate<String, JsonNode> kafkaTemplate;

    @Value("${messaging.kafka.topics.route-order-events}")
    private String routeOrderEventsTopic;

    public void publish(OutboxEventEntity event) {
        try {
            kafkaTemplate.send(MessageBuilder.withPayload(event.getPayload())
                    .setHeader(KafkaHeaders.TOPIC, routeOrderEventsTopic)
                    .setHeader(KafkaHeaders.KEY, event.getAggregateId())
                    .setHeader("eventId", event.getId().toString())
                    .setHeader("eventType", event.getEventType())
                    .setHeader("aggregateType", event.getAggregateType())
                    .setHeader("aggregateId", event.getAggregateId())
                    .build()).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing outbox event", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Failed to publish outbox event: " + event.getId(), exception);
        }
    }
}
