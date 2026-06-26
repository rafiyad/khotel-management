package com.kaptaitourist.kaptaitourist.Room.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.Room.domain.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomListResponseDto {
    private String message;
    private int totalRecords;
    private List<Room> roomData;
}
