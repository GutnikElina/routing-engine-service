package com.logistics.routing.adapter.out.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {


    @Query(value = """
            SELECT *
            FROM routing.outbox_events
            WHERE status = :status
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
            """, nativeQuery = true)
    List<OutboxEventEntity> lockNextBatch(
            @Param("status") String status,
            @Param("limit") int limit
    );
}
