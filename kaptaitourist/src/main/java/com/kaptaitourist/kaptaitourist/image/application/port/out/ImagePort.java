package com.kaptaitourist.kaptaitourist.image.application.port.out;

import com.kaptaitourist.kaptaitourist.image.domain.Image;
import reactor.core.publisher.Mono;

public interface ImagePort {
    Mono<Image> save(Image image);
    Mono<Image> findById(String id);
}
