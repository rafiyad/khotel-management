package com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.image.domain.Image;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageSingleResponseDto {
    private String message;
    private Image imageData;
}
