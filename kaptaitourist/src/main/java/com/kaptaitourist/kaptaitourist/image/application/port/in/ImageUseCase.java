package com.kaptaitourist.kaptaitourist.image.application.port.in;


import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageListRequestDto;
import com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto.ImageListResponseDto;
import reactor.core.publisher.Mono;

public interface ImageUseCase {
    
    Mono<ImageListResponseDto> saveImage(ImageListRequestDto imageListRequestDto);
    Mono<ImageListResponseDto> findById(String id);
}
