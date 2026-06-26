package com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence;

import com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.entity.HotelEntity;
import com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.repository.HotelRepository;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelPort;
import com.kaptaitourist.kaptaitourist.hotel.domain.Hotel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class HotelAdapter implements HotelPort {

    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;

    @Override
    public Mono<Hotel> save(Hotel hotel) {
        log.info("Saving hotel to db: {}", hotel.getName());
        return hotelRepository.save(modelMapper.map(hotel, HotelEntity.class))
                .map(entity -> modelMapper.map(entity, Hotel.class))
                .doOnSuccess(saved -> log.info("Saved hotel with id: {}", saved.getId()))
                .doOnError(e -> log.error("Error saving hotel: {}", e.getMessage()));
    }

    @Override
    public Flux<Hotel> findAll() {
        log.info("Finding all hotels");
        return hotelRepository.findAll()
                .map(entity -> modelMapper.map(entity, Hotel.class))
                .doOnError(e -> log.error("Error finding hotels: {}", e.getMessage()));
    }

    @Override
    public Mono<Hotel> findById(String id) {
        log.info("Finding hotel id: {}", id);
        return hotelRepository.findById(id)
                .map(entity -> modelMapper.map(entity, Hotel.class))
                .doOnError(e -> log.error("Error finding hotel id {}: {}", id, e.getMessage()));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        log.info("Deleting hotel id: {}", id);
        return hotelRepository.deleteById(id)
                .doOnError(e -> log.error("Error deleting hotel id {}: {}", id, e.getMessage()));
    }

    @Override
    public Mono<Boolean> existsById(String id) {
        return hotelRepository.existsById(id);
    }
}
