package com.logistics.routing.adapter.in.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;     
import org.springframework.http.HttpStatus;

import com.logistics.routing.adapter.in.web.dto.request.CreateRouteRequest;
import com.logistics.routing.adapter.in.web.dto.response.CreateRouteResponse;
import com.logistics.routing.application.route.create.CreateRouteUseCase;
import com.logistics.routing.adapter.in.web.mapper.RouteWebMapper;
import jakarta.validation.Valid;
import com.logistics.routing.application.route.create.CreateRouteCommand;
import com.logistics.routing.application.route.create.CreateRouteResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {
    private final CreateRouteUseCase createRouteUseCase;
    private final RouteWebMapper routeWebMapper;
    @PostMapping
    public ResponseEntity<CreateRouteResponse> createRoute(@Valid @RequestBody CreateRouteRequest request) {
        CreateRouteCommand command = routeWebMapper.toCommand(request);
        CreateRouteResult result = createRouteUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(routeWebMapper.toResponse(result));
    }
}
