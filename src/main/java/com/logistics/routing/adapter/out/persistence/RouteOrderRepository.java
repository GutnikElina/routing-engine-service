package com.logistics.routing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface RouteOrderRepository extends JpaRepository<RouteOrderEntity, UUID> {
}
