package com.logistics.routing.adapter.in.web;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.logistics.routing.adapter.in.web.generated.api.RoutesApi;
import com.logistics.routing.adapter.in.web.generated.model.CreateRouteRequest;
import com.logistics.routing.adapter.in.web.generated.model.CreateRouteResponse;
import com.logistics.routing.adapter.in.web.mapper.RouteWebMapper;
import com.logistics.routing.application.route.create.CreateRouteUseCase;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RouteController implements RoutesApi {
    private final CreateRouteUseCase createRouteUseCase;
    private final RouteWebMapper routeWebMapper;

    @Override
    public ResponseEntity<CreateRouteResponse> createRoute(CreateRouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeWebMapper.toResponse(createRouteUseCase.execute(routeWebMapper.toCommand(request))));
    }
}
