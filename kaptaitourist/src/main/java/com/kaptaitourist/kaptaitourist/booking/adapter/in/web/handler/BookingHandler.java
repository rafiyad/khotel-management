package com.kaptaitourist.kaptaitourist.booking.adapter.in.web.handler;

import com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto.BookingRequestDto;
import com.kaptaitourist.kaptaitourist.booking.application.port.in.BookingUseCase;
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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingHandler {

    private final BookingUseCase bookingUseCase;
    private final GlobalExceptionHandler exceptionHandler;

    // ─────────────────────────────── Create ──────────────────────────────────

    public Mono<ServerResponse> createBooking(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        String roomId = request.pathVariable("roomId");
        return request.principal()
                .switchIfEmpty(Mono.error(new ValidationException("Authentication required")))
                .flatMap(principal -> request.bodyToMono(BookingRequestDto.class)
                        .switchIfEmpty(Mono.error(new ValidationException("Request body is required")))
                        .flatMap(dto -> bookingUseCase.createBooking(hotelId, roomId, principal.getName(), dto)))
                .flatMap(result -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Availability ────────────────────────────

    public Mono<ServerResponse> checkAvailability(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        String roomId = request.pathVariable("roomId");
        final LocalDate checkIn;
        final LocalDate checkOut;
        final int units;
        try {
            checkIn = LocalDate.parse(request.queryParam("checkIn")
                    .orElseThrow(() -> new ValidationException("checkIn query param is required (yyyy-MM-dd)")));
            checkOut = LocalDate.parse(request.queryParam("checkOut")
                    .orElseThrow(() -> new ValidationException("checkOut query param is required (yyyy-MM-dd)")));
            units = request.queryParam("units").map(Integer::parseInt).orElse(1);
        } catch (ValidationException e) {
            return exceptionHandler.handle(e);
        } catch (DateTimeParseException | NumberFormatException e) {
            return exceptionHandler.handle(new ValidationException("Invalid query parameter: " + e.getMessage()));
        }
        return bookingUseCase.checkAvailability(hotelId, roomId, checkIn, checkOut, units)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── List by hotel ───────────────────────────

    public Mono<ServerResponse> getHotelBookings(ServerRequest request) {
        return bookingUseCase.findAllByHotelId(request.pathVariable("hotelId"))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Get one ─────────────────────────────────

    public Mono<ServerResponse> getBookingById(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        String bookingId = request.pathVariable("bookingId");
        return bookingUseCase.findById(hotelId, bookingId)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Cancel ──────────────────────────────────

    public Mono<ServerResponse> cancelBooking(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        String bookingId = request.pathVariable("bookingId");
        return bookingUseCase.cancelBooking(hotelId, bookingId)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }
}
