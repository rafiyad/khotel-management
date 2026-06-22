package com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageListResponseDto {
    private String id;
    private String hotelId;
    private String fileUrl;
    private String fileName;
    private int fileSizeBytes;
    private String mimeType;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private boolean isDeleted;
}
