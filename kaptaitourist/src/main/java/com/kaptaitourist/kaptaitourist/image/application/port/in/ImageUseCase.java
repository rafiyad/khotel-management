package com.kaptaitourist.kaptaitourist.image.application.port.in;

import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageListRequestDto;
import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageListResponseDto;
import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageSingleResponseDto;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ImageUseCase {

    Mono<ImageListResponseDto> saveImage(ImageListRequestDto dto, List<FilePart> files);

    Mono<ImageSingleResponseDto> findById(String id, String hotelId);

    Mono<ImageListResponseDto> findAllByHotelId(String hotelId);

    Mono<ImageSingleResponseDto> updateImage(String id, String hotelId, FilePart newFile, Boolean isThumbnail, String updatedBy);

    Mono<Void> deleteImage(String id, String hotelId);

    Mono<Void> deleteAllByHotelId(String hotelId);

    /** Marks a hotel-gallery image as the primary (cover), unsetting any prior primary for the hotel. */
    Mono<ImageSingleResponseDto> setHotelImagePrimary(String hotelId, String imageId);

    /** Marks a room image as the room's primary (cover), unsetting any prior primary for the room. */
    Mono<ImageSingleResponseDto> setRoomImagePrimary(String hotelId, String roomId, String imageId);

    // ---- Room-scoped ----

    Mono<ImageListResponseDto> findAllByRoomId(String roomId);

    Mono<Void> deleteRoomImage(String roomId, String id);

    Mono<Void> deleteAllByRoomId(String roomId);
}