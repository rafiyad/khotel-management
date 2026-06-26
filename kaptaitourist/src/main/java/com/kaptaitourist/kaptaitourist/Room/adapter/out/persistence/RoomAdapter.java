package com.kaptaitourist.kaptaitourist.Room.adapter.out.persistence;

import com.kaptaitourist.kaptaitourist.Room.adapter.out.persistence.entity.RoomEntity;
import com.kaptaitourist.kaptaitourist.Room.adapter.out.persistence.repository.RoomRepository;
import com.kaptaitourist.kaptaitourist.Room.application.port.out.RoomPort;
import com.kaptaitourist.kaptaitourist.Room.domain.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class RoomAdapter implements RoomPort {

    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;

    @Override
    public Mono<Room> save(Room room) {
        log.info("Saving room to db: {} (hotelId={})", room.getRoomName(), room.getHotelId());
        return roomRepository.save(modelMapper.map(room, RoomEntity.class))
                .map(entity -> modelMapper.map(entity, Room.class))
                .doOnSuccess(saved -> log.info("Saved room with id: {}", saved.getId()))
                .doOnError(e -> log.error("Error saving room: {}", e.getMessage()));
    }

    @Override
    public Flux<Room> findAllByHotelId(String hotelId) {
        log.info("Finding all rooms for hotelId: {}", hotelId);
        return roomRepository.findAllByHotelId(hotelId)
                .map(entity -> modelMapper.map(entity, Room.class))
                .doOnError(e -> log.error("Error finding rooms for hotelId {}: {}", hotelId, e.getMessage()));
    }

    @Override
    public Mono<Room> findByIdAndHotelId(String roomId, String hotelId) {
        log.info("Finding room id: {} for hotelId: {}", roomId, hotelId);
        return roomRepository.findByIdAndHotelId(roomId, hotelId)
                .map(entity -> modelMapper.map(entity, Room.class))
                .doOnError(e -> log.error("Error finding room id {} for hotelId {}: {}", roomId, hotelId, e.getMessage()));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        log.info("Deleting room id: {}", id);
        return roomRepository.deleteById(id)
                .doOnError(e -> log.error("Error deleting room id {}: {}", id, e.getMessage()));
    }
}
