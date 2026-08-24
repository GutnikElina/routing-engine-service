package com.logistics.routing.adapter.out.persistence.outbox;

import com.logistics.routing.adapter.out.messaging.kafka.KafkaOutboxEventProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaOutboxEventProducer kafkaOutboxEventProducer;
    private final Clock clock;

    @Value("${outbox.publisher.batch-size}")
    private final int batchSize;

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay}")
    @SchedulerLock(name = "outbox-event-publisher")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, batchSize)
        );

        for (OutboxEventEntity event : events) {
            try {
                kafkaOutboxEventProducer.publish(event);
                event.setStatus(OutboxEventStatus.SENT);
                event.setProcessedAt(Instant.now(clock));
            } catch (RuntimeException exception) {
                log.atWarn().setCause(exception).log(
                        "Failed to publish outbox event {}; it will be retried",
                        event.getId()
                );
            }
        }
    }
}
