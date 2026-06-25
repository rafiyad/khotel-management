package com.kaptaitourist.kaptaitourist.core.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedImage {
    private byte[] originalBytes;
    private String originalFileName;
    private byte[] thumbnailBytes;   // null if isThumbnail = false
    private String thumbnailFileName; // null if isThumbnail = false
    private String contentType;
}