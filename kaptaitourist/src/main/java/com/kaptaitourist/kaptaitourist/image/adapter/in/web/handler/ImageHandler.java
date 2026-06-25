package com.kaptaitourist.kaptaitourist.image.adapter.in.web.handler;

import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.core.exception.handler.GlobalExceptionHandler;
import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageListRequestDto;
import com.kaptaitourist.kaptaitourist.image.application.port.in.ImageUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ImageHandler {

    private final ImageUseCase imageUseCase;
    private final GlobalExceptionHandler exceptionHandler;

    // ─────────────────────────────── Save (multi-file) ───────────────────────

    public Mono<ServerResponse> saveImage(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");

        return request.multipartData()
                .flatMap(multipart -> {
                    List<FilePart> files = multipart.get("files") == null ? List.of()
                            : multipart.get("files").stream()
                            .filter(p -> p instanceof FilePart)
                            .map(p -> (FilePart) p)
                            .toList();

                    if (files.isEmpty())
                        return Mono.error(new ValidationException("At least one file is required under the 'files' key"));

                    String createdBy  = firstFormValue(multipart.getFirst("createdBy"));
                    String thumbStr   = firstFormValue(multipart.getFirst("isThumbnail"));

                    ImageListRequestDto dto = ImageListRequestDto.builder()
                            .hotelId(hotelId)
                            .createdBy(createdBy)
                            .isThumbnail(thumbStr != null ? Boolean.parseBoolean(thumbStr) : true)
                            .build();

                    return imageUseCase.saveImage(dto, files);
                })
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Find by ID ──────────────────────────────

    public Mono<ServerResponse> getImageById(ServerRequest request) {
        String imageId = request.pathVariable("imageId");
        String hotelId = request.pathVariable("hotelId");

        return imageUseCase.findById(imageId, hotelId)
                .flatMap(responseDto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(responseDto))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Find all by hotel ───────────────────────

    public Mono<ServerResponse> getAllByHotelId(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");

        return imageUseCase.findAllByHotelId(hotelId)
                .flatMap(responseDto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(responseDto))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Update ──────────────────────────────────

    public Mono<ServerResponse> updateImage(ServerRequest request) {
        String imageId = request.pathVariable("imageId");
        String hotelId = request.pathVariable("hotelId");

        return request.multipartData()
                .flatMap(multipart -> {
                    FilePart newFile = multipart.get("file") == null ? null
                            : multipart.get("file").stream()
                            .filter(p -> p instanceof FilePart)
                            .map(p -> (FilePart) p)
                            .findFirst()
                            .orElse(null);

                    if (newFile == null)
                        return Mono.error(new ValidationException("A replacement file is required under the 'file' key"));

                    String updatedBy  = firstFormValue(multipart.getFirst("updatedBy"));
                    String thumbStr   = firstFormValue(multipart.getFirst("isThumbnail"));
                    Boolean isThumbnail = thumbStr != null ? Boolean.parseBoolean(thumbStr) : null;

                    return imageUseCase.updateImage(imageId, hotelId, newFile, isThumbnail, updatedBy);
                })
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Delete one ──────────────────────────────

    public Mono<ServerResponse> deleteImage(ServerRequest request) {
        String imageId = request.pathVariable("imageId");
        String hotelId = request.pathVariable("hotelId");

        return imageUseCase.deleteImage(imageId, hotelId)
                .then(ServerResponse.noContent().build())
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Delete all by hotel ─────────────────────

    public Mono<ServerResponse> deleteAllByHotelId(ServerRequest request) {
        String hotelId = request.pathVariable("hotelId");

        return imageUseCase.deleteAllByHotelId(hotelId)
                .then(ServerResponse.noContent().build())
                .onErrorResume(exceptionHandler::handle);
    }

    // ─────────────────────────────── Helpers ─────────────────────────────────

    private String firstFormValue(Part part) {
        if (part instanceof FormFieldPart ffp) return ffp.value();
        return null;
    }
}