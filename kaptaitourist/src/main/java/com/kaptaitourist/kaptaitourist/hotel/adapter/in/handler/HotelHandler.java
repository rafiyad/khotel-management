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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

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
        // ?search= : case-insensitive hotel-name substring (absent -> all).
        // ?facility=<id>&facility=<id> : keep only hotels that have ALL the given facilities.
        // ?checkIn=&checkOut=&guests= : keep only hotels with a room free for the dates (yyyy-MM-dd).
        // ?page= (0-based) & ?size= : pagination (defaults 0 / 10, size capped at 100).
        String search = request.queryParam("search").orElse(null);
        List<String> facilities = request.queryParams().getOrDefault("facility", List.of());
        final int page;
        final int size;
        final LocalDate checkIn;
        final LocalDate checkOut;
        final Integer guests;
        try {
            page = request.queryParam("page").map(Integer::parseInt).orElse(0);
            size = request.queryParam("size").map(Integer::parseInt).orElse(10);
            guests = request.queryParam("guests").map(Integer::parseInt).orElse(null);
            checkIn = request.queryParam("checkIn").map(LocalDate::parse).orElse(null);
            checkOut = request.queryParam("checkOut").map(LocalDate::parse).orElse(null);
        } catch (NumberFormatException e) {
            return exceptionHandler.handle(new ValidationException("page, size and guests must be integers"));
        } catch (DateTimeParseException e) {
            return exceptionHandler.handle(new ValidationException("checkIn/checkOut must be dates (yyyy-MM-dd)"));
        }
        return hotelUseCase.findAll(search, facilities, checkIn, checkOut, guests, page, size)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Owner / Admin lists ─────────────────────

    public Mono<ServerResponse> getOwnerHotels(ServerRequest request) {
        return request.principal()
                .switchIfEmpty(Mono.error(new ValidationException("Authentication required")))
                .flatMap(principal -> hotelUseCase.findOwnerHotels(principal.getName()))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    public Mono<ServerResponse> getAdminHotels(ServerRequest request) {
        return hotelUseCase.findAdminHotels()
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
