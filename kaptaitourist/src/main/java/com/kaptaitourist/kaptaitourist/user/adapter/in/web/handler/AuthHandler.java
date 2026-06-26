package com.kaptaitourist.kaptaitourist.user.adapter.in.web.handler;

import com.kaptaitourist.kaptaitourist.core.exception.InvalidCredentialsException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.core.exception.handler.GlobalExceptionHandler;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.LoginRequestDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.RegisterRequestDto;
import com.kaptaitourist.kaptaitourist.user.application.port.in.UserUseCase;
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
public class AuthHandler {

    private final UserUseCase userUseCase;
    private final GlobalExceptionHandler exceptionHandler;

    // ─────────────────────────────── Register ────────────────────────────────

    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(RegisterRequestDto.class)
                .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                .flatMap(userUseCase::register)
                .flatMap(result -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Login ───────────────────────────────────

    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(LoginRequestDto.class)
                .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                .flatMap(userUseCase::login)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Current user ────────────────────────────

    public Mono<ServerResponse> me(ServerRequest request) {
        return request.principal()
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Not authenticated")))
                .map(Principal::getName)
                .flatMap(userUseCase::getCurrentUser)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Admin: list users ───────────────────────

    public Mono<ServerResponse> listUsers(ServerRequest request) {
        return userUseCase.findAll()
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Admin: promote ──────────────────────────

    public Mono<ServerResponse> promote(ServerRequest request) {
        String userId = request.pathVariable("userId");
        return userUseCase.promoteToHotelOwner(userId)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }
}
