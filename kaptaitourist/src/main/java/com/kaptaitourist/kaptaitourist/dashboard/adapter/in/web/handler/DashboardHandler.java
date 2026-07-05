package com.kaptaitourist.kaptaitourist.dashboard.adapter.in.web.handler;

import com.kaptaitourist.kaptaitourist.core.exception.handler.GlobalExceptionHandler;
import com.kaptaitourist.kaptaitourist.dashboard.application.port.in.DashboardUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class DashboardHandler {

    private final DashboardUseCase dashboardUseCase;
    private final GlobalExceptionHandler exceptionHandler;

    public Mono<ServerResponse> getDashboard(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        return dashboardUseCase.getDashboard(hotelId)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> getAdminDashboard(ServerRequest request) {
        return dashboardUseCase.getAdminDashboard()
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }
}
