package com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.in.web.handler;

import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.core.exception.handler.GlobalExceptionHandler;
import com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.in.web.dto.*;
import com.kaptaitourist.kaptaitourist.supabaseauthmodule.application.port.in.SupabaseUserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthenticationRouterHandler {
    private final SupabaseUserUseCase supabaseUserUseCase;
    private final GlobalExceptionHandler exceptionHandler;

    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(RegisterRequestDto.class)
                .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                .flatMap(supabaseUserUseCase::register)
                .flatMap(result -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(LoginRequestDto.class)
                .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                .flatMap(supabaseUserUseCase::login)
                .flatMap(result -> ServerResponse.status(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> forgotPassword(ServerRequest request) {
        return request.bodyToMono(ForgotPasswordRequestDto.class)
                .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                .flatMap(supabaseUserUseCase::forgotPassword)
                .flatMap(result -> ServerResponse.status(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> resetPassword(ServerRequest request) {
        return request.bodyToMono(ResetPasswordRequestDto.class)
                .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                .flatMap(supabaseUserUseCase::resetPassword)
                .flatMap(result -> ServerResponse.status(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }
}
