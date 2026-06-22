package com.kaptaitourist.kaptaitourist.image.adapter.out.persistence;


import com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.entity.ImageEntity;
import com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.repository.ImageRepository;
import com.kaptaitourist.kaptaitourist.image.application.port.out.ImagePort;
import com.kaptaitourist.kaptaitourist.image.domain.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class ImageAdapter implements ImagePort {

    private final ImageRepository imageRepository;
    private final ModelMapper modelMapper;

    @Override
    public Mono<Image> save(Image image) {
        log.info("Saving image to db: {}", image);
        return imageRepository.save(modelMapper.map(image, ImageEntity.class))
                .map(imageEntity -> modelMapper.map(imageEntity, Image.class))
                .doOnSuccess(savedImage -> log.info("Saved image to db: {}", savedImage))
                .doOnError(error -> log.error("Error saving image to db: {}", error.getMessage()));
    }

    @Override
    public Mono<Image> findById(String id) {
        return imageRepository.findById(id)
                .doOnRequest(request -> log.info("Finding image from db with id: {}", id))
                .map(imageEntity -> modelMapper.map(imageEntity, Image.class))
                .doOnSuccess(image -> log.info("Mapped ImageEntity to KImage: {}", image))
                .doOnError(error -> log.error("Error finding image with id {}: {}", id, error.getMessage()));
    }
}
