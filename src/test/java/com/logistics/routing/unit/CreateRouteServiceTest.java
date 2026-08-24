package com.logistics.routing.unit;

import com.logistics.routing.application.port.out.RouteOrderPersistencePort;
import com.logistics.routing.application.port.out.OutboxEventPort;
import com.logistics.routing.application.route.create.CreateRouteResult;
import com.logistics.routing.application.route.create.CreateRouteService;
import com.logistics.routing.application.route.create.RouteCreatedEvent;
import com.logistics.routing.domain.route.exception.InvalidRouteDraftException;
import com.logistics.routing.domain.route.model.RouteOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.logistics.routing.testdata.CreateRouteTestData.ORDER_NUMBER;
import static com.logistics.routing.testdata.CreateRouteTestData.commandWithInvalidFirstWaypoint;
import static com.logistics.routing.testdata.CreateRouteTestData.validCreateRouteCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CreateRouteServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

    @Mock
    private RouteOrderPersistencePort routeOrderPersistencePort;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Captor
    private ArgumentCaptor<RouteOrder> routeOrderCaptor;

    @Captor
    private ArgumentCaptor<RouteCreatedEvent> routeCreatedEventCaptor;

    private CreateRouteService createRouteService;

    @BeforeEach
    void setUp() {
        createRouteService = new CreateRouteService(
                routeOrderPersistencePort,
                outboxEventPort,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_shouldPersistRoutePublishCreationEventAndReturnCreatedRoute() {
        CreateRouteResult result = createRouteService.execute(validCreateRouteCommand());

        verify(routeOrderPersistencePort).save(routeOrderCaptor.capture());
        verify(outboxEventPort).append(routeCreatedEventCaptor.capture());

        RouteOrder routeOrder = routeOrderCaptor.getValue();
        RouteCreatedEvent routeCreatedEvent = routeCreatedEventCaptor.getValue();

        assertThat(result)
                .extracting(
                        CreateRouteResult::routeId,
                        CreateRouteResult::orderNumber,
                        CreateRouteResult::status,
                        CreateRouteResult::createdAt
                )
                .containsExactly(routeOrder.getId(), ORDER_NUMBER, "DRAFT", NOW);
        assertThat(routeOrder.getWaypoints()).hasSize(2);
        assertThat(routeCreatedEvent)
                .extracting(
                        RouteCreatedEvent::routeId,
                        RouteCreatedEvent::orderNumber,
                        RouteCreatedEvent::status,
                        RouteCreatedEvent::occurredAt
                )
                .containsExactly(routeOrder.getId(), ORDER_NUMBER, routeOrder.getStatus(), NOW);
        assertThat(routeCreatedEvent.eventId()).isNotNull();
    }

    @Test
    void execute_shouldNotCallPortsWhenRouteDraftIsInvalid() {
        assertThatThrownBy(() -> createRouteService.execute(commandWithInvalidFirstWaypoint()))
                .isInstanceOf(InvalidRouteDraftException.class)
                .hasMessage("the first waypoint must be a PICKUP");

        verifyNoInteractions(routeOrderPersistencePort, outboxEventPort);
    }
}
