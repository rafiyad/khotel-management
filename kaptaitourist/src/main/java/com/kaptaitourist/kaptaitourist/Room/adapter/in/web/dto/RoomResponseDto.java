package com.kaptaitourist.kaptaitourist.Room.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.Room.domain.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomResponseDto {
    private String message;
    private Room roomData;
}
