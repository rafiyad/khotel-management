package com.kaptaitourist.kaptaitourist.core.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.List;

public interface StorageService {
    Mono<UploadedImageUrls> uploadImage(FilePart file, String identifier, boolean generateThumbnail);
    Mono<Void> deleteImage(String imageUrl);
}
