package com.kaptaitourist.kaptaitourist.image.adapter.out.persistence;

import com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.entity.ImageEntity;
import com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.repository.ImageRepository;
import com.kaptaitourist.kaptaitourist.image.application.port.out.ImagePort;
import com.kaptaitourist.kaptaitourist.image.domain.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Component
@Slf4j
@RequiredArgsConstructor
public class ImageAdapter implements ImagePort {

    private final ImageRepository imageRepository;
    private final ModelMapper modelMapper;

    @Override
    public Mono<Image> save(Image image) {
        log.info("Saving image to db: {}", image.getFileName());
        return imageRepository.save(modelMapper.map(image, ImageEntity.class))
                .map(entity -> modelMapper.map(entity, Image.class))
                .doOnSuccess(saved -> log.info("Saved image with id: {}", saved.getId()))
                .doOnError(e -> log.error("Error saving image: {}", e.getMessage()));
    }

    @Override
    public Flux<Image> findAllByHotelId(String hotelId) {
        log.info("Finding all images for hotelId: {}", hotelId);
        return imageRepository.findAllByHotelId(hotelId)
                .map(entity -> modelMapper.map(entity, Image.class))
                .doOnError(e -> log.error("Error finding images for hotelId {}: {}", hotelId, e.getMessage()));
    }

    @Override
    public Flux<Image> findAllByRoomId(String roomId) {
        log.info("Finding all images for roomId: {}", roomId);
        return imageRepository.findAllByRoomId(roomId)
                .map(entity -> modelMapper.map(entity, Image.class))
                .doOnError(e -> log.error("Error finding images for roomId {}: {}", roomId, e.getMessage()));
    }

    @Override
    public Flux<Image> findAllByRoomIdIn(Collection<String> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Flux.empty();
        }
        return imageRepository.findAllByRoomIdIn(roomIds)
                .map(entity -> modelMapper.map(entity, Image.class))
                .doOnError(e -> log.error("Error finding images for roomIds {}: {}", roomIds, e.getMessage()));
    }

    @Override
    public Mono<Image> findByIdAndHotelId(String id, String hotelId) {
        log.info("Finding image id: {} for hotelId: {}", id, hotelId);
        return imageRepository.findByIdAndHotelId(id, hotelId)
                .map(entity -> modelMapper.map(entity, Image.class))
                .doOnError(e -> log.error("Error finding image id {} for hotelId {}: {}", id, hotelId, e.getMessage()));
    }

    @Override
    public Mono<Void> DeleteByIdAndHotelId(String id, String hotelId) {
        log.info("Deleting image id: {} for hotelId: {}", id, hotelId);
        return imageRepository.DeleteByIdAndHotelId(id, hotelId)
                .doOnError(e -> log.error("Error deleting image id {} for hotelId {}: {}", id, hotelId, e.getMessage()));
    }

    @Override
    public Mono<Void> DeleteAllByHotelId(String hotelId) {
        log.info("Deleting all images for hotelId: {}", hotelId);
        return imageRepository.DeleteAllByHotelId(hotelId)
                .doOnError(e -> log.error("Error deleting all images for hotelId {}: {}", hotelId, e.getMessage()));
    }

    @Override
    public Mono<Image> findByIdAndRoomId(String id, String roomId) {
        log.info("Finding image id: {} for roomId: {}", id, roomId);
        return imageRepository.findByIdAndRoomId(id, roomId)
                .map(entity -> modelMapper.map(entity, Image.class))
                .doOnError(e -> log.error("Error finding image id {} for roomId {}: {}", id, roomId, e.getMessage()));
    }

    @Override
    public Mono<Void> deleteByIdAndRoomId(String id, String roomId) {
        log.info("Deleting image id: {} for roomId: {}", id, roomId);
        return imageRepository.deleteByIdAndRoomId(id, roomId)
                .doOnError(e -> log.error("Error deleting image id {} for roomId {}: {}", id, roomId, e.getMessage()));
    }

    @Override
    public Mono<Void> deleteAllByRoomId(String roomId) {
        log.info("Deleting all images for roomId: {}", roomId);
        return imageRepository.deleteAllByRoomId(roomId)
                .doOnError(e -> log.error("Error deleting all images for roomId {}: {}", roomId, e.getMessage()));
    }
}