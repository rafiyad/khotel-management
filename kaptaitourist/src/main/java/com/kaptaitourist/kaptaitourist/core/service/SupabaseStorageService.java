package com.kaptaitourist.kaptaitourist.core.service;

import com.kaptaitourist.kaptaitourist.core.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Profile("dev")
public class SupabaseStorageService implements StorageService {

    private final WebClient webClient;
    private final String supabaseUrl;
    private final String bucket;
    private final ImageUtil imageUtil;

    public SupabaseStorageService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.anon-key}") String anonKey,
            @Value("${supabase.storage.bucket}") String bucket,
            ImageUtil imageUtil) {
        this.supabaseUrl = supabaseUrl;
        this.bucket = bucket;
        this.imageUtil = imageUtil;
        this.webClient = WebClient.builder()
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader("Authorization", "Bearer " + anonKey)
                .defaultHeader("apikey", anonKey)
                .build();
    }

    @Override
    public Mono<UploadedImageUrls> uploadImage(FilePart file, String identifier, boolean generateThumbnail) {
        String contentType = file.headers().getContentType() != null
                ? file.headers().getContentType().toString()
                : "image/jpeg";

        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    byte[] rawBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(rawBytes);
                    DataBufferUtils.release(dataBuffer);

                    // offload blocking Thumbnailator work off the event loop
                    return Mono.fromCallable(() ->
                            imageUtil.process(rawBytes, contentType, file.filename(), identifier, generateThumbnail)
                    ).subscribeOn(Schedulers.boundedElastic());
                })
                .flatMap(processed -> {
                    // webp thumbnail is just full-size duplicate; skip it
                    boolean thumbnailAvailable = processed.getThumbnailBytes() != null
                            && !processed.getContentType().equals("image/webp");

                    if (!thumbnailAvailable && generateThumbnail
                            && processed.getContentType().equals("image/webp")) {
                        log.warn("Thumbnail generation skipped for WebP file '{}' — Thumbnailator cannot encode WebP",
                                processed.getOriginalFileName());
                    }

                    Mono<String> originalUpload = upload(
                            processed.getOriginalBytes(),
                            processed.getOriginalFileName(),
                            processed.getContentType());

                    Mono<String> thumbnailUpload = thumbnailAvailable
                            ? upload(processed.getThumbnailBytes(),
                            processed.getThumbnailFileName(),
                            processed.getContentType())
                            : Mono.just("__NONE__");

                    return Mono.zip(originalUpload, thumbnailUpload)
                            .map(tuple -> UploadedImageUrls.builder()
                                    .imageUrl(tuple.getT1())
                                    .imageName(processed.getOriginalFileName())
                                    .thumbnailUrl(tuple.getT2().equals("__NONE__") ? null : tuple.getT2())
                                    .fileSizeBytes(processed.getOriginalBytes().length)
                                    .mimeType(processed.getContentType())
                                    .build());
                });
    }

    private Mono<String> upload(byte[] bytes, String fileName, String contentType) {
        return webClient.post()
                .uri("/object/" + bucket + "/" + fileName)
                .contentType(MediaType.parseMediaType(contentType))
                .header("x-upsert", "true")
                .body(BodyInserters.fromValue(bytes))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RuntimeException("Supabase upload failed: " + body))))
                .bodyToMono(String.class)
                .map(r -> supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + fileName);
    }

    @Override
    public Mono<Void> deleteImage(String imageUrl) {
        String prefix = supabaseUrl + "/storage/v1/object/public/" + bucket + "/";
        if (imageUrl == null || !imageUrl.startsWith(prefix))
            return Mono.empty();

        String fileName = imageUrl.substring(prefix.length());

        return webClient.method(HttpMethod.DELETE)
                .uri("/object/" + bucket)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("prefixes", List.of(fileName)))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RuntimeException("Supabase delete failed: " + body))))
                .bodyToMono(String.class)
                .then()
                .onErrorResume(e -> {
                    log.error("Failed to delete image from Supabase: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}