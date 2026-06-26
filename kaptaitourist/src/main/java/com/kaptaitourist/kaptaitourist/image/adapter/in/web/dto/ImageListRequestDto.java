package com.kaptaitourist.kaptaitourist.image.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageListRequestDto {
    private String hotelId;
    private String roomId;       // NULL = hotel-level upload; set = room-level upload
    private Boolean isThumbnail;
    private String createdBy;
}
