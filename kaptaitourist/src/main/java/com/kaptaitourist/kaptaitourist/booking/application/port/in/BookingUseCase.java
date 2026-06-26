package com.kaptaitourist.kaptaitourist.booking.application.port.in;

import com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto.AvailabilityResponseDto;
import com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto.BookingListResponseDto;
import com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto.BookingRequestDto;
import com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto.BookingResponseDto;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface BookingUseCase {

    Mono<BookingResponseDto> createBooking(String hotelId, String roomId, BookingRequestDto dto);

    Mono<AvailabilityResponseDto> checkAvailability(String hotelId, String roomId,
                                                    LocalDate checkIn, LocalDate checkOut, int units);

    Mono<BookingListResponseDto> findAllByHotelId(String hotelId);

    Mono<BookingResponseDto> findById(String hotelId, String bookingId);

    Mono<BookingResponseDto> cancelBooking(String hotelId, String bookingId);
}
