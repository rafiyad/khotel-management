package com.kaptaitourist.kaptaitourist.image.adapter.in.web.handler;

import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageListRequestDto;
import com.kaptaitourist.kaptaitourist.image.application.port.in.ImageUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ImageHandler {
    private final ImageUseCase imageUseCase;


    public Mono<ServerResponse> saveImage(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");
        if (hotelId == null || hotelId.isBlank()) {
            return Mono.error(new IllegalArgumentException("hotelId is required"));
        }

        return request.multipartData()
                .flatMapMany(parts -> Flux.fromIterable(parts.getOrDefault("file", List.of())))
                .cast(FilePart.class)
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("file part is required")))
                .map(filePart -> ImageListRequestDto.builder()
                        .hotelId(hotelId)
                        .name(filePart.filename())
                        .file(filePart)
                        .build())
                .flatMap(imageUseCase::saveImage)
                .flatMap(responseDto -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(responseDto))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }

    public Mono<ServerResponse> getImageById(ServerRequest request) {
        String imageId = request.pathVariable("imageId");
        return imageUseCase.findById(imageId)
                .flatMap(responseDto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(responseDto))
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}
