package com.kaptaitourist.kaptaitourist.image.application.service;

import com.kaptaitourist.kaptaitourist.core.exception.ImageNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.core.service.StorageService;
import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageListRequestDto;
import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageListResponseDto;
import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageSingleResponseDto;
import com.kaptaitourist.kaptaitourist.image.application.port.in.ImageUseCase;
import com.kaptaitourist.kaptaitourist.image.application.port.out.ImagePort;
import com.kaptaitourist.kaptaitourist.image.domain.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageService implements ImageUseCase {
    private static final int MAX_IMAGES_PER_UPLOAD = 5;

    private final ImagePort imagePort;
    private final StorageService storageService;

    // -------------------- Save (single/multi file) ---------------------------

    @Override
    public Mono<ImageListResponseDto> saveImage(ImageListRequestDto dto, List<FilePart> files) {
        if (dto.getHotelId() == null || dto.getHotelId().isBlank())
            return Mono.error(new ValidationException("Hotel ID is required"));
        if (files == null || files.isEmpty())
            return Mono.error(new ValidationException("At least one file is required"));

        if (files.size() > MAX_IMAGES_PER_UPLOAD)
            return Mono.error(new ValidationException("Maximum " + MAX_IMAGES_PER_UPLOAD + " images per upload"));


        boolean generateThumbnail = dto.getIsThumbnail() == null || dto.getIsThumbnail();

        // concatMap preserves upload order; AtomicInteger assigns stable displayOrder
        AtomicInteger orderCounter = new AtomicInteger(0);

        return Flux.fromIterable(files)
                .flatMap(file -> {
                    int order = orderCounter.getAndIncrement();
                    return storageService.uploadImage(file, dto.getHotelId(), generateThumbnail)
                            .map(uploaded -> Image.builder()
                                    .hotelId(dto.getHotelId())
                                    .roomId(dto.getRoomId())
                                    .fileUrl(uploaded.getImageUrl())
                                    .fileName(uploaded.getImageName())
                                    .fileSizeBytes(uploaded.getFileSizeBytes())
                                    .thumbnailUrl(uploaded.getThumbnailUrl())
                                    .isPrimary(false)
                                    .displayOrder(order)
                                    .mimeType(uploaded.getMimeType())
                                    .createdBy(dto.getCreatedBy())
                                    .createdAt(LocalDateTime.now())
                                    .build())
                            .flatMap(imagePort::save);
                })
                .collectList()
                .map(savedList -> ImageListResponseDto.builder()
                        .message("Images uploaded successfully")
                        .totalRecords(savedList.size())
                        .imageData(savedList)
                        .build())
                .doOnSuccess(r -> log.info("Saved {} image(s) for hotelId: {}", r.getTotalRecords(), dto.getHotelId()))
                .doOnError(e -> log.error("Error saving images for hotelId {}: {}", dto.getHotelId(), e.getMessage()));
    }

    // --------------------------------- Find by ID --------------------------------------

    @Override
    public Mono<ImageSingleResponseDto> findById(String id, String hotelId) {
        return imagePort.findByIdAndHotelId(id, hotelId)
                .switchIfEmpty(Mono.error(new ImageNotFoundException(
                        "Image not found with id: " + id + " for hotel: " + hotelId)))
                .map(image -> ImageSingleResponseDto.builder()
                        .message("Image retrieved successfully")
                        .imageData(image)
                        .build())
                .doOnError(e -> log.error("Error finding image id {} for hotelId {}: {}", id, hotelId, e.getMessage()));
    }

    // ------------------------------- Find all images by hotel id ---------------------------------------

    @Override
    public Mono<ImageListResponseDto> findAllByHotelId(String hotelId) {
        return imagePort.findAllByHotelId(hotelId)
                .collectList()
                .map(list -> ImageListResponseDto.builder()
                        .message("Images retrieved successfully")
                        .totalRecords(list.size())
                        .imageData(list)
                        .build())
                .doOnError(e -> log.error("Error finding images for hotelId {}: {}", hotelId, e.getMessage()));
    }

    // ----------------------------------- Update -------------------------------------------

    @Override
    public Mono<ImageSingleResponseDto> updateImage(String id, String hotelId, FilePart newFile,
                                                    Boolean isThumbnail, String updatedBy) {
        return imagePort.findByIdAndHotelId(id, hotelId)
                .switchIfEmpty(Mono.error(new ImageNotFoundException(
                        "Image not found with id: " + id + " for hotel: " + hotelId)))
                .flatMap(existing -> {
                    boolean generateThumbnail = isThumbnail != null ? isThumbnail : existing.getThumbnailUrl() != null;
                    return storageService.uploadImage(newFile, hotelId, generateThumbnail)
                            .flatMap(uploaded -> {
                                Mono<Void> deleteOld = Mono.when(
                                        storageService.deleteImage(existing.getFileUrl()),
                                        existing.getThumbnailUrl() != null
                                                ? storageService.deleteImage(existing.getThumbnailUrl())
                                                : Mono.empty()
                                ).onErrorResume(e -> {
                                    log.warn("Could not delete old image files from storage: {}", e.getMessage());
                                    return Mono.empty();
                                });

                                Image updated = Image.builder()
                                        .id(existing.getId())
                                        .hotelId(existing.getHotelId())
                                        .fileUrl(uploaded.getImageUrl())
                                        .fileName(uploaded.getImageName())
                                        .fileSizeBytes(uploaded.getFileSizeBytes())
                                        .thumbnailUrl(uploaded.getThumbnailUrl())
                                        .mimeType(uploaded.getMimeType())
                                        .isPrimary(existing.isPrimary())
                                        .displayOrder(existing.getDisplayOrder())
                                        .version(existing.getVersion())
                                        .createdBy(existing.getCreatedBy())
                                        .createdAt(existing.getCreatedAt())
                                        .updatedBy(updatedBy)
                                        .updatedAt(LocalDateTime.now())
                                        .build();

                                return deleteOld.then(imagePort.save(updated));
                            });
                })
                .map(saved -> ImageSingleResponseDto.builder()
                        .message("Image updated successfully")
                        .imageData(saved)
                        .build())
                .doOnError(e -> log.error("Error updating image id {} for hotelId {}: {}", id, hotelId, e.getMessage()));
    }

    // -------------------------------------- Delete specific image ---------------------------

    @Override
    public Mono<Void> deleteImage(String id, String hotelId) {
        return imagePort.findByIdAndHotelId(id, hotelId)
                .switchIfEmpty(Mono.error(new ImageNotFoundException(
                        "Image not found with id: " + id + " for hotel: " + hotelId)))
                .flatMap(existing -> Mono.when(
                        storageService.deleteImage(existing.getFileUrl()),
                        existing.getThumbnailUrl() != null
                                ? storageService.deleteImage(existing.getThumbnailUrl())
                                : Mono.empty()
                ).onErrorResume(e -> {
                    log.warn("Could not delete image file from storage: {}", e.getMessage());
                    return Mono.empty();
                }).then(imagePort.DeleteByIdAndHotelId(id, hotelId)))
                .doOnSuccess(v -> log.info("Deleted image id: {} for hotelId: {}", id, hotelId))
                .doOnError(e -> log.error("Error deleting image id {} for hotelId {}: {}", id, hotelId, e.getMessage()));
    }

    // ----------------------- Delete all image of a hotel ----------------------------------------------
    @Override
    public Mono<Void> deleteAllByHotelId(String hotelId) {
        return imagePort.findAllByHotelId(hotelId)
                .flatMap(image -> Mono.when(
                        storageService.deleteImage(image.getFileUrl()),
                        image.getThumbnailUrl() != null
                                ? storageService.deleteImage(image.getThumbnailUrl())
                                : Mono.empty()
                ).onErrorResume(e -> {
                    log.warn("Could not delete image files from storage: {}", e.getMessage());
                    return Mono.empty();
                }))
                .then(imagePort.DeleteAllByHotelId(hotelId))
                .doOnSuccess(v -> log.info("Deleted all images for hotelId: {}", hotelId))
                .doOnError(e -> log.error("Error deleting all images for hotelId {}: {}", hotelId, e.getMessage()));
    }

    // ----------------------- Room-scoped image operations -------------------------------

    @Override
    public Mono<ImageListResponseDto> findAllByRoomId(String roomId) {
        return imagePort.findAllByRoomId(roomId)
                .collectList()
                .map(list -> ImageListResponseDto.builder()
                        .message("Images retrieved successfully")
                        .totalRecords(list.size())
                        .imageData(list)
                        .build())
                .doOnError(e -> log.error("Error finding images for roomId {}: {}", roomId, e.getMessage()));
    }

    @Override
    public Mono<Void> deleteRoomImage(String roomId, String id) {
        return imagePort.findByIdAndRoomId(id, roomId)
                .switchIfEmpty(Mono.error(new ImageNotFoundException(
                        "Image not found with id: " + id + " for room: " + roomId)))
                .flatMap(existing -> Mono.when(
                        storageService.deleteImage(existing.getFileUrl()),
                        existing.getThumbnailUrl() != null
                                ? storageService.deleteImage(existing.getThumbnailUrl())
                                : Mono.empty()
                ).onErrorResume(e -> {
                    log.warn("Could not delete image file from storage: {}", e.getMessage());
                    return Mono.empty();
                }).then(imagePort.deleteByIdAndRoomId(id, roomId)))
                .doOnSuccess(v -> log.info("Deleted image id: {} for roomId: {}", id, roomId))
                .doOnError(e -> log.error("Error deleting image id {} for roomId {}: {}", id, roomId, e.getMessage()));
    }

    @Override
    public Mono<Void> deleteAllByRoomId(String roomId) {
        return imagePort.findAllByRoomId(roomId)
                .flatMap(image -> Mono.when(
                        storageService.deleteImage(image.getFileUrl()),
                        image.getThumbnailUrl() != null
                                ? storageService.deleteImage(image.getThumbnailUrl())
                                : Mono.empty()
                ).onErrorResume(e -> {
                    log.warn("Could not delete image files from storage: {}", e.getMessage());
                    return Mono.empty();
                }))
                .then(imagePort.deleteAllByRoomId(roomId))
                .doOnSuccess(v -> log.info("Deleted all images for roomId: {}", roomId))
                .doOnError(e -> log.error("Error deleting all images for roomId {}: {}", roomId, e.getMessage()));
    }
}