package com.kaptaitourist.kaptaitourist.facility.adapter.in.web.handler;

import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.core.exception.handler.GlobalExceptionHandler;
import com.kaptaitourist.kaptaitourist.core.security.UserContextService;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.FacilityAssignmentRequestDto;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.FacilityRequestDto;
import com.kaptaitourist.kaptaitourist.facility.application.port.in.FacilityUseCase;
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
public class FacilityHandler {

    private final FacilityUseCase facilityUseCase;
    private final GlobalExceptionHandler exceptionHandler;
    private final UserContextService userContextService;

    // ─────────────────────────────── Catalog ─────────────────────────────────

    public Mono<ServerResponse> createFacility(ServerRequest request) {
        return Mono.zip(request.bodyToMono(FacilityRequestDto.class)
                        .switchIfEmpty(Mono.error(new ValidationException("Request body is required"))),
                        userContextService.getAuthContext())
                .flatMap(t -> facilityUseCase.createFacility(t.getT1(), t.getT2().userId()))
                .flatMap(result -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> getAllFacilities(ServerRequest request) {
        return facilityUseCase.findAll()
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> getFacilityById(ServerRequest request) {
        return facilityUseCase.findById(request.pathVariable("facilityId"))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> updateFacility(ServerRequest request) {
        String facilityId = request.pathVariable("facilityId");
        return Mono.zip(request.bodyToMono(FacilityRequestDto.class)
                        .switchIfEmpty(Mono.error(new ValidationException("Request body is required"))),
                        userContextService.getAuthContext())
                .flatMap(t -> facilityUseCase.updateFacility(facilityId, t.getT1(), t.getT2().userId(), t.getT2().isAdmin()))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> deleteFacility(ServerRequest request) {
        String facilityId = request.pathVariable("facilityId");
        return userContextService.getAuthContext()
                .flatMap(ctx -> facilityUseCase.deleteFacility(facilityId, ctx.userId(), ctx.isAdmin()))
                .then(ServerResponse.noContent().build())
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Hotel assignment ────────────────────────

    public Mono<ServerResponse> assignToHotel(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        return request.bodyToMono(FacilityAssignmentRequestDto.class)
                .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                .flatMap(dto -> facilityUseCase.assignToHotel(hotelId, dto))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> getHotelFacilities(ServerRequest request) {
        return facilityUseCase.findHotelFacilities(request.pathVariable("hotelId"))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> unassignFromHotel(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        String facilityId = request.pathVariable("facilityId");
        return facilityUseCase.unassignFromHotel(hotelId, facilityId)
                .then(ServerResponse.noContent().build())
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Room assignment ─────────────────────────

    public Mono<ServerResponse> assignToRoom(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        String roomId = request.pathVariable("roomId");
        return request.bodyToMono(FacilityAssignmentRequestDto.class)
                .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                .flatMap(dto -> facilityUseCase.assignToRoom(hotelId, roomId, dto))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> getRoomFacilities(ServerRequest request) {
        return facilityUseCase.findRoomFacilities(request.pathVariable("roomId"))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> unassignFromRoom(ServerRequest request) {
        String roomId = request.pathVariable("roomId");
        String facilityId = request.pathVariable("facilityId");
        return facilityUseCase.unassignFromRoom(roomId, facilityId)
                .then(ServerResponse.noContent().build())
                .onErrorResume(exceptionHandler::handle);
    }
}
