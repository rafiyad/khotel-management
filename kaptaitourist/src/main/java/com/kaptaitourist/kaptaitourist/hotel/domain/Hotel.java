package com.kaptaitourist.kaptaitourist.hotel.domain;


import com.kaptaitourist.kaptaitourist.Room.domain.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String imageUrl;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String mobile;
    private String email;
    private String website;
    private String address;
    private String googleMapUrl;
    private List<Room> rooms;

}
