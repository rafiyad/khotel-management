package com.kaptaitourist.kaptaitourist.booking.adapter.out.persistence;

import com.kaptaitourist.kaptaitourist.booking.adapter.out.persistence.entity.BookingEntity;
import com.kaptaitourist.kaptaitourist.booking.adapter.out.persistence.repository.BookingRepository;
import com.kaptaitourist.kaptaitourist.booking.application.port.out.BookingPort;
import com.kaptaitourist.kaptaitourist.booking.domain.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingAdapter implements BookingPort {

    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;

    @Override
    public Mono<Booking> save(Booking booking) {
        return bookingRepository.save(modelMapper.map(booking, BookingEntity.class))
                .map(entity -> modelMapper.map(entity, Booking.class))
                .doOnError(e -> log.error("Error saving booking: {}", e.getMessage()));
    }

    @Override
    public Flux<Booking> findAllByHotelId(String hotelId) {
        return bookingRepository.findAllByHotelId(hotelId)
                .map(entity -> modelMapper.map(entity, Booking.class));
    }

    @Override
    public Mono<Booking> findByIdAndHotelId(String bookingId, String hotelId) {
        return bookingRepository.findByIdAndHotelId(bookingId, hotelId)
                .map(entity -> modelMapper.map(entity, Booking.class));
    }

    @Override
    public Mono<Integer> lockRoomTotalUnits(String roomId) {
        return bookingRepository.lockRoomTotalUnits(roomId);
    }

    @Override
    public Mono<Long> sumBookedUnits(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return bookingRepository.sumBookedUnits(roomId, checkIn, checkOut);
    }
}
