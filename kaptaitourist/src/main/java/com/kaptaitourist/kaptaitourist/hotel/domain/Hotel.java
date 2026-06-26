package com.kaptaitourist.kaptaitourist.hotel.domain;


import com.kaptaitourist.kaptaitourist.Room.domain.Room;
import com.kaptaitourist.kaptaitourist.image.domain.Image;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Hotel {
    private String id;
    private String name;
    private String description;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String mobile;
    private String email;
    private String website;
    private String address;
    private String googleMapUrl;
    private Long version;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private List<Image> images;
    private List<Room> rooms;
}
