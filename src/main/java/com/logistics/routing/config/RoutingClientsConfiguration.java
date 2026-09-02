package com.logistics.routing.config;

import com.logistics.routing.adapter.out.osrm.OsrmResponseMapper;
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
            RoutingProperties properties,
            OsrmResponseMapper responseMapper
    ) {
        Map<TransportType, RoutingClient> clients = new EnumMap<>(TransportType.class);

        clients.put(
                TransportType.TRUCK,
                osrmClient(requireOsrmUrl(properties, TransportType.TRUCK), properties, responseMapper)
        );
        clients.put(
                TransportType.TRAIN,
                osrmClient(requireOsrmUrl(properties, TransportType.TRAIN), properties, responseMapper)
        );
        clients.put(
                TransportType.VESSEL,
                osrmClient(requireOsrmUrl(properties, TransportType.VESSEL), properties, responseMapper)
        );
        clients.put(TransportType.PLANE, new StraightLineRoutingClient(properties.getPlaneAverageSpeedKmh()));

        return clients;
    }

    private String requireOsrmUrl(RoutingProperties properties, TransportType transportType) {
        String baseUrl = properties.getOsrmBaseUrls().get(transportType);
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("OSRM base URL is not configured for transport type: " + transportType);
        }
        return baseUrl;
    }

    private OsrmRoutingClient osrmClient(
            String baseUrl,
            RoutingProperties properties,
            OsrmResponseMapper responseMapper
    ) {
        return new OsrmRoutingClient(
                buildRestClient(baseUrl, properties),
                OSRM_PROFILE,
                properties.getRouteOverview(),
                responseMapper
        );
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
