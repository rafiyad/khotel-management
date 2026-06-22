package com.kaptaitourist.kaptaitourist.image.application.service;

import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageListRequestDto;
import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageListResponseDto;
import com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.repository.ImageRepository;
import com.kaptaitourist.kaptaitourist.image.application.port.in.ImageUseCase;
import com.kaptaitourist.kaptaitourist.image.application.port.out.ImagePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageService implements ImageUseCase {

    private final ImagePort imagePort;
    private final ModelMapper modelMapper;

    @Override
    public Mono<ImageListResponseDto> saveImage(ImageListRequestDto imageListRequestDto) {
        return null;
    }

    @Override
    public Mono<ImageListResponseDto> findById(String id) {
        return imagePort.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Image not found with id: " + id)))
                .map(image -> modelMapper.map(image, ImageListResponseDto.class))
                .doOnNext(response -> log.info("Image found with id: {}", id))
                .doOnError(error -> log.error("Error finding image with id {}: {}", id, error.getMessage()));
    }
}
