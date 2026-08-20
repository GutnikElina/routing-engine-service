package com.logistics.routing.application.route.create;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class CreateRouteService implements CreateRouteUseCase {


    @Override
    @Transactional
    public CreateRouteResult execute(CreateRouteCommand command) {
        
        return null;
    }

}
