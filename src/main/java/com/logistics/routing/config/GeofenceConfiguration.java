package com.logistics.routing.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeofenceProperties.class)
public class GeofenceConfiguration {
}
