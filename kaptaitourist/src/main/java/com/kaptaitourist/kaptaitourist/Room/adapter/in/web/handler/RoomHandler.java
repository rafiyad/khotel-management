package com.kaptaitourist.kaptaitourist.Room.adapter.in.web.handler;

import com.kaptaitourist.kaptaitourist.Room.adapter.in.web.dto.RoomRequestDto;
import com.kaptaitourist.kaptaitourist.Room.application.port.in.RoomUseCase;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.core.exception.handler.GlobalExceptionHandler;
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
public class RoomHandler {

    private final RoomUseCase roomUseCase;
    private final GlobalExceptionHandler exceptionHandler;

    // ─────────────────────────────── Create ──────────────────────────────────

    public Mono<ServerResponse> createRoom(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        return request.bodyToMono(RoomRequestDto.class)
                .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                .flatMap(dto -> roomUseCase.createRoom(hotelId, dto))
                .flatMap(result -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Find all by hotel ───────────────────────

    public Mono<ServerResponse> getAllRooms(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        return roomUseCase.findAllByHotelId(hotelId)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Find by id ──────────────────────────────

    public Mono<ServerResponse> getRoomById(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        String roomId = request.pathVariable("roomId");
        return roomUseCase.findById(hotelId, roomId)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Update ──────────────────────────────────

    public Mono<ServerResponse> updateRoom(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        String roomId = request.pathVariable("roomId");
        return request.bodyToMono(RoomRequestDto.class)
                .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                .flatMap(dto -> roomUseCase.updateRoom(hotelId, roomId, dto))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Delete ──────────────────────────────────

    public Mono<ServerResponse> deleteRoom(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        String roomId = request.pathVariable("roomId");
        return roomUseCase.deleteRoom(hotelId, roomId)
                .then(ServerResponse.noContent().build())
                .onErrorResume(exceptionHandler::handle);
    }
}
