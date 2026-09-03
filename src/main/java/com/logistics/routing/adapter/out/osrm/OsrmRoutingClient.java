package com.logistics.routing.adapter.out.osrm;

import com.logistics.routing.adapter.out.osrm.dto.OsrmRouteRequest;
import com.logistics.routing.adapter.out.osrm.dto.OsrmRouteResponse;
import com.logistics.routing.adapter.out.osrm.dto.OsrmTableRequest;
import com.logistics.routing.adapter.out.osrm.dto.OsrmTableResponse;
import com.logistics.routing.adapter.out.routing.RoutingClient;
import com.logistics.routing.application.routing.DistanceMatrix;
import com.logistics.routing.application.routing.GeoCoordinate;
import com.logistics.routing.application.routing.RouteGeometry;
import com.logistics.routing.domain.route.exception.RoutingEngineException;
import com.logistics.routing.domain.route.exception.RoutingEngineUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class OsrmRoutingClient implements RoutingClient {

    private final RestClient restClient;
    private final String profile;
    private final String routeOverview;

    @Override
    @Retry(name = "osrm")
    @CircuitBreaker(name = "osrm")
    public DistanceMatrix getDistanceMatrix(List<GeoCoordinate> coordinates) {
        OsrmTableRequest request = OsrmTableRequest.of(toOsrmCoordinates(coordinates));

        try {
            OsrmTableResponse response = restClient.post()
                    .uri("/table/v1/{profile}", profile)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                        throw toHttpException(res);
                    })
                    .body(OsrmTableResponse.class);

            return OsrmResponseMapper.toDistanceMatrix(response);
        } catch (RoutingEngineException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            log.atDebug().setCause(exception).log("OSRM table request failed due to connectivity issue");
            throw new RoutingEngineUnavailableException("OSRM service unavailable", exception);
        } catch (RestClientException exception) {
            if (isConnectivityIssue(exception)) {
                log.atDebug().setCause(exception).log("OSRM table request failed due to connectivity issue");
                throw new RoutingEngineUnavailableException("OSRM service unavailable", exception);
            }
            log.atDebug().setCause(exception).log("OSRM table request failed");
            throw new RoutingEngineException("OSRM table request failed", exception);
        }
    }

    @Override
    @Retry(name = "osrm")
    @CircuitBreaker(name = "osrm")
    public RouteGeometry getRouteGeometry(List<GeoCoordinate> waypointsInOrder) {
        OsrmRouteRequest request = OsrmRouteRequest.of(
                toOsrmCoordinates(waypointsInOrder),
                routeOverview
        );

        try {
            OsrmRouteResponse response = restClient.post()
                    .uri("/route/v1/{profile}", profile)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                        throw toHttpException(res);
                    })
                    .body(OsrmRouteResponse.class);

            return OsrmResponseMapper.toRouteGeometry(response);
        } catch (RoutingEngineException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            log.atDebug().setCause(exception).log("OSRM route request failed due to connectivity issue");
            throw new RoutingEngineUnavailableException("OSRM service unavailable", exception);
        } catch (RestClientException exception) {
            if (isConnectivityIssue(exception)) {
                log.atDebug().setCause(exception).log("OSRM route request failed due to connectivity issue");
                throw new RoutingEngineUnavailableException("OSRM service unavailable", exception);
            }
            log.atDebug().setCause(exception).log("OSRM route request failed");
            throw new RoutingEngineException("OSRM route request failed", exception);
        }
    }

    private List<List<Double>> toOsrmCoordinates(List<GeoCoordinate> coordinates) {
        return coordinates.stream()
                .map(coordinate -> List.of(
                        OsrmNumberConverter.toOsrmDouble(coordinate.longitude()),
                        OsrmNumberConverter.toOsrmDouble(coordinate.latitude())
                ))
                .toList();
    }

    private RoutingEngineException toHttpException(ClientHttpResponse response) throws IOException {
        log.atDebug().log("OSRM HTTP error: status={}", response.getStatusCode());
        return new RoutingEngineException("OSRM request failed with HTTP " + response.getStatusCode().value());
    }

    private boolean isConnectivityIssue(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ResourceAccessException
                    || current instanceof SocketTimeoutException
                    || current instanceof ConnectException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
