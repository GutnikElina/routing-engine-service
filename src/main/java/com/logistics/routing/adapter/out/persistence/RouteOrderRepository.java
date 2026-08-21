package com.logistics.routing.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface RouteOrderRepository extends JpaRepository<RouteOrderEntity, UUID> {
}
