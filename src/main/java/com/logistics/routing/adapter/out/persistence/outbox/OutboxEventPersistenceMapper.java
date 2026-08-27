package com.logistics.routing.adapter.out.persistence.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routing.application.route.create.RouteCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPersistenceMapper {

    private static final String ROUTE_ORDER_AGGREGATE_TYPE = "RouteOrder";
    private static final String ROUTE_CREATED_EVENT_TYPE = "RouteCreatedEvent";

    private final ObjectMapper objectMapper;

    public OutboxEventEntity toEntity(RouteCreatedEvent event) {
        return OutboxEventEntity.builder()
                .aggregateType(ROUTE_ORDER_AGGREGATE_TYPE)
                .aggregateId(event.routeId().toString())
                .eventType(ROUTE_CREATED_EVENT_TYPE)
                .payload(objectMapper.valueToTree(event))
                .status(OutboxEventStatus.PENDING)
                .createdAt(event.occurredAt())
                .build();
    }
}
