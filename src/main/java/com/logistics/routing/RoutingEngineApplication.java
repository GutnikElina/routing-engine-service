package com.logistics.routing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RoutingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoutingEngineApplication.class, args);
    }
}
