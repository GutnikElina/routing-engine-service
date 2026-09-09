package com.logistics.routing.adapter.out.persistence.outbox;

import com.logistics.routing.application.geofence.generate.GeofenceZoneCreatedEvent;
import com.logistics.routing.application.port.out.OutboxEventPort;
import com.logistics.routing.application.route.create.RouteCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements OutboxEventPort {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPersistenceMapper outboxEventPersistenceMapper;

    @Override
    public void append(RouteCreatedEvent event) {
        outboxEventRepository.save(outboxEventPersistenceMapper.toEntity(event));
    }

    @Override
    public void append(GeofenceZoneCreatedEvent event) {
        outboxEventRepository.save(outboxEventPersistenceMapper.toEntity(event));
    }
}
