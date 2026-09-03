package com.logistics.routing.config;

import com.logistics.routing.adapter.out.osrm.OsrmRoutingClient;
import com.logistics.routing.adapter.out.routing.RoutingClient;
import com.logistics.routing.adapter.out.routing.RoutingProperties;
import com.logistics.routing.adapter.out.straightline.StraightLineRoutingClient;
import com.logistics.routing.domain.route.model.enums.TransportType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.EnumMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(RoutingProperties.class)
public class RoutingClientsConfiguration {

    private static final String OSRM_PROFILE = "driving";

    @Bean
    Map<TransportType, RoutingClient> routingClients(
            RoutingClient truckOsrmClient,
            RoutingClient trainOsrmClient,
            RoutingClient vesselOsrmClient,
            RoutingClient planeStraightLineClient
    ) {
        Map<TransportType, RoutingClient> clients = new EnumMap<>(TransportType.class);
        clients.put(TransportType.TRUCK, truckOsrmClient);
        clients.put(TransportType.TRAIN, trainOsrmClient);
        clients.put(TransportType.VESSEL, vesselOsrmClient);
        clients.put(TransportType.PLANE, planeStraightLineClient);
        return clients;
    }

    @Bean
    RoutingClient truckOsrmClient(RoutingProperties properties) {
        return createOsrmClient(properties, TransportType.TRUCK);
    }

    @Bean
    RoutingClient trainOsrmClient(RoutingProperties properties) {
        return createOsrmClient(properties, TransportType.TRAIN);
    }

    @Bean
    RoutingClient vesselOsrmClient(RoutingProperties properties) {
        return createOsrmClient(properties, TransportType.VESSEL);
    }

    @Bean
    RoutingClient planeStraightLineClient(RoutingProperties properties) {
        return new StraightLineRoutingClient(properties.getPlaneAverageSpeedKmh());
    }

    private OsrmRoutingClient createOsrmClient(RoutingProperties properties, TransportType transportType) {
        return new OsrmRoutingClient(
                buildRestClient(requireOsrmUrl(properties, transportType), properties),
                OSRM_PROFILE,
                properties.getRouteOverview()
        );
    }

    private String requireOsrmUrl(RoutingProperties properties, TransportType transportType) {
        String baseUrl = properties.getOsrmBaseUrls().get(transportType);
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("OSRM base URL is not configured for transport type: " + transportType);
        }
        return baseUrl;
    }

    private RestClient buildRestClient(String baseUrl, RoutingProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(properties.getConnectTimeout())
                .withReadTimeout(properties.getReadTimeout());
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.simple().build(settings);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
