package com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.handler;

import com.kaptaitourist.kaptaitourist.core.exception.InvalidCredentialsException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.core.exception.handler.GlobalExceptionHandler;
import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto.RegisterOwnerRequestDto;
import com.kaptaitourist.kaptaitourist.ownerrequest.application.port.in.OwnerRequestUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Component
@Slf4j
@RequiredArgsConstructor
public class OwnerRequestHandler {

    private final OwnerRequestUseCase ownerRequestUseCase;
    private final GlobalExceptionHandler exceptionHandler;

    public Mono<ServerResponse> registerOwner(ServerRequest request) {
        return request.bodyToMono(RegisterOwnerRequestDto.class)
                .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                .flatMap(ownerRequestUseCase::registerOwner)
                .flatMap(result -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> list(ServerRequest request) {
        String status = request.queryParam("status").orElse(null);
        return ownerRequestUseCase.list(status)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> approve(ServerRequest request) {
        String requestId = request.pathVariable("requestId");
        return request.principal()
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Not authenticated")))
                .map(Principal::getName)
                .flatMap(adminId -> ownerRequestUseCase.approve(requestId, adminId))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> reject(ServerRequest request) {
        String requestId = request.pathVariable("requestId");
        return request.principal()
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Not authenticated")))
                .map(Principal::getName)
                .flatMap(adminId -> ownerRequestUseCase.reject(requestId, adminId))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }
}
