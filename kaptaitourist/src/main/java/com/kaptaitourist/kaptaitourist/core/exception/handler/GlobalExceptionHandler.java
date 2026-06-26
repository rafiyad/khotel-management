package com.kaptaitourist.kaptaitourist.core.exception.handler;

import com.kaptaitourist.kaptaitourist.core.exception.BookingNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.FacilityNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.HotelNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.ImageNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.RoomNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.core.exception.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ----------- Used by functional router handlers via onErrorResume --------------------

    public Mono<ServerResponse> handle(Throwable ex) {
        if (ex instanceof ValidationException) {
            return buildResponse(HttpStatus.BAD_REQUEST, "Validation Error", ex.getMessage());
        }
        if (ex instanceof ImageNotFoundException) {
            return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
        }
        if (ex instanceof HotelNotFoundException) {
            return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
        }
        if (ex instanceof RoomNotFoundException) {
            return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
        }
        if (ex instanceof FacilityNotFoundException) {
            return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
        }
        if (ex instanceof BookingNotFoundException) {
            return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
        }
        ex.printStackTrace();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage());
    }

    private Mono<ServerResponse> buildResponse(HttpStatus status, String error, String message) {
        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    // -------------- @RestControllerAdvice fallbacks (annotated controllers, if any) ------------------------

    @ExceptionHandler(ValidationException.class)
    public Mono<ErrorResponse> handleValidation(ValidationException ex) {
        return Mono.just(ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public Mono<ErrorResponse> handleNotFound(ImageNotFoundException ex) {
        return Mono.just(ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public Mono<ErrorResponse> handleGeneric(Exception ex) {
        ex.printStackTrace();
        return Mono.just(ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }
}