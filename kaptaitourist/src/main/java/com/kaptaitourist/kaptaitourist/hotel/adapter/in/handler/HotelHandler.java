package com.kaptaitourist.kaptaitourist.hotel.adapter.in.handler;

import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.core.exception.handler.GlobalExceptionHandler;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelRequestDto;
import com.kaptaitourist.kaptaitourist.hotel.application.port.in.HotelUseCase;
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
public class HotelHandler {

    private final HotelUseCase hotelUseCase;
    private final GlobalExceptionHandler exceptionHandler;

    // ─────────────────────────────── Create ──────────────────────────────────

    public Mono<ServerResponse> createHotel(ServerRequest request) {
        return request.principal()
                .switchIfEmpty(Mono.error(new ValidationException("Authentication required")))
                .flatMap(principal -> request.bodyToMono(HotelRequestDto.class)
                        .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                        .flatMap(dto -> hotelUseCase.createHotel(dto, principal.getName())))
                .flatMap(result -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Find all ────────────────────────────────

    public Mono<ServerResponse> getAllHotels(ServerRequest request) {
        return hotelUseCase.findAll()
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Find by id ──────────────────────────────

    public Mono<ServerResponse> getHotelById(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        return hotelUseCase.findById(hotelId)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Update ──────────────────────────────────

    public Mono<ServerResponse> updateHotel(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        return request.principal()
                .switchIfEmpty(Mono.error(new ValidationException("Authentication required")))
                .flatMap(principal -> request.bodyToMono(HotelRequestDto.class)
                        .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                        .flatMap(dto -> hotelUseCase.updateHotel(hotelId, dto, principal.getName())))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Delete ──────────────────────────────────

    public Mono<ServerResponse> deleteHotel(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        return hotelUseCase.deleteHotel(hotelId)
                .then(ServerResponse.noContent().build())
                .onErrorResume(exceptionHandler::handle);
    }
}
