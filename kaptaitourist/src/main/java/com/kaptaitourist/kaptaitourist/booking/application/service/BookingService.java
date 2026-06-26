package com.kaptaitourist.kaptaitourist.booking.application.service;

import com.kaptaitourist.kaptaitourist.Room.application.port.out.RoomPort;
import com.kaptaitourist.kaptaitourist.Room.domain.Room;
import com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto.AvailabilityResponseDto;
import com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto.BookingListResponseDto;
import com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto.BookingRequestDto;
import com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto.BookingResponseDto;
import com.kaptaitourist.kaptaitourist.booking.application.port.in.BookingUseCase;
import com.kaptaitourist.kaptaitourist.booking.application.port.out.BookingPort;
import com.kaptaitourist.kaptaitourist.booking.domain.Booking;
import com.kaptaitourist.kaptaitourist.core.exception.BookingNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.RoomNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService implements BookingUseCase {

    private final BookingPort bookingPort;
    private final RoomPort roomPort;

    // ----------------------------------- Create (transactional) ---------------------------

    @Override
    @Transactional
    public Mono<BookingResponseDto> createBooking(String hotelId, String roomId, BookingRequestDto dto) {
        if (dto.getGuestName() == null || dto.getGuestName().isBlank())
            return Mono.error(new ValidationException("guestName is required"));
        if (dto.getCheckIn() == null || dto.getCheckOut() == null)
            return Mono.error(new ValidationException("checkIn and checkOut are required (yyyy-MM-dd)"));
        if (!dto.getCheckOut().isAfter(dto.getCheckIn()))
            return Mono.error(new ValidationException("checkOut must be after checkIn"));
        if (dto.getCheckIn().isBefore(LocalDate.now()))
            return Mono.error(new ValidationException("checkIn cannot be in the past"));
        final int units = dto.getUnits() != null ? dto.getUnits() : 1;
        if (units < 1)
            return Mono.error(new ValidationException("units must be at least 1"));

        return roomPort.findByIdAndHotelId(roomId, hotelId)
                .switchIfEmpty(Mono.error(new RoomNotFoundException(
                        "Room not found with id: " + roomId + " for hotel: " + hotelId)))
                // Lock the room row, then count overlapping booked units. The lock (held until
                // the transaction commits) serializes concurrent bookings so we cannot oversell.
                .flatMap(room -> bookingPort.lockRoomTotalUnits(roomId)
                        .flatMap(totalUnits -> bookingPort.sumBookedUnits(roomId, dto.getCheckIn(), dto.getCheckOut())
                                .flatMap(booked -> {
                                    long available = totalUnits - booked;
                                    if (available < units)
                                        return Mono.error(new ValidationException(
                                                "Only " + Math.max(0, available) + " unit(s) of '" + room.getRoomName()
                                                        + "' available for " + dto.getCheckIn() + " to " + dto.getCheckOut()));

                                    Booking booking = Booking.builder()
                                            .hotelId(hotelId)
                                            .roomId(roomId)
                                            .checkIn(dto.getCheckIn())
                                            .checkOut(dto.getCheckOut())
                                            .units(units)
                                            .guestName(dto.getGuestName())
                                            .guestPhone(dto.getGuestPhone())
                                            .guestEmail(dto.getGuestEmail())
                                            .numberOfGuests(dto.getNumberOfGuests())
                                            .status("CONFIRMED")
                                            .totalPrice(computeTotal(room, dto.getCheckIn(), dto.getCheckOut(), units))
                                            .createdBy(dto.getCreatedBy())
                                            .createdAt(LocalDateTime.now())
                                            .build();
                                    return bookingPort.save(booking);
                                })))
                .map(saved -> BookingResponseDto.builder()
                        .message("Booking confirmed")
                        .bookingData(saved)
                        .build())
                .doOnSuccess(r -> log.info("Created booking id: {} for room {}", r.getBookingData().getId(), roomId))
                .doOnError(e -> log.error("Error creating booking for room {}: {}", roomId, e.getMessage()));
    }

    // ----------------------------------- Availability -------------------------------------

    @Override
    public Mono<AvailabilityResponseDto> checkAvailability(String hotelId, String roomId,
                                                           LocalDate checkIn, LocalDate checkOut, int units) {
        if (checkIn == null || checkOut == null)
            return Mono.error(new ValidationException("checkIn and checkOut are required (yyyy-MM-dd)"));
        if (!checkOut.isAfter(checkIn))
            return Mono.error(new ValidationException("checkOut must be after checkIn"));
        final int requested = units < 1 ? 1 : units;

        return roomPort.findByIdAndHotelId(roomId, hotelId)
                .switchIfEmpty(Mono.error(new RoomNotFoundException(
                        "Room not found with id: " + roomId + " for hotel: " + hotelId)))
                .flatMap(room -> bookingPort.sumBookedUnits(roomId, checkIn, checkOut)
                        .map(booked -> {
                            long available = room.getTotalUnits() - booked;
                            return AvailabilityResponseDto.builder()
                                    .roomId(roomId)
                                    .checkIn(checkIn)
                                    .checkOut(checkOut)
                                    .totalUnits(room.getTotalUnits())
                                    .bookedUnits(booked)
                                    .availableUnits(Math.max(0, available))
                                    .requestedUnits(requested)
                                    .available(available >= requested)
                                    .build();
                        }));
    }

    // ----------------------------------- Read ---------------------------------------------

    @Override
    public Mono<BookingListResponseDto> findAllByHotelId(String hotelId) {
        return bookingPort.findAllByHotelId(hotelId)
                .collectList()
                .map(list -> BookingListResponseDto.builder()
                        .message("Bookings retrieved successfully")
                        .totalRecords(list.size())
                        .bookingData(list)
                        .build());
    }

    @Override
    public Mono<BookingResponseDto> findById(String hotelId, String bookingId) {
        return bookingPort.findByIdAndHotelId(bookingId, hotelId)
                .switchIfEmpty(Mono.error(new BookingNotFoundException(
                        "Booking not found with id: " + bookingId + " for hotel: " + hotelId)))
                .map(booking -> BookingResponseDto.builder()
                        .message("Booking retrieved successfully")
                        .bookingData(booking)
                        .build());
    }

    // ----------------------------------- Cancel -------------------------------------------

    @Override
    public Mono<BookingResponseDto> cancelBooking(String hotelId, String bookingId) {
        return bookingPort.findByIdAndHotelId(bookingId, hotelId)
                .switchIfEmpty(Mono.error(new BookingNotFoundException(
                        "Booking not found with id: " + bookingId + " for hotel: " + hotelId)))
                .flatMap(booking -> {
                    if ("CANCELLED".equalsIgnoreCase(booking.getStatus()))
                        return Mono.just(booking); // idempotent
                    booking.setStatus("CANCELLED");
                    booking.setUpdatedAt(LocalDateTime.now());
                    return bookingPort.save(booking);
                })
                .map(saved -> BookingResponseDto.builder()
                        .message("Booking cancelled")
                        .bookingData(saved)
                        .build())
                .doOnSuccess(r -> log.info("Cancelled booking id: {}", bookingId));
    }

    // ----------------------------------- Helpers ------------------------------------------

    private BigDecimal computeTotal(Room room, LocalDate checkIn, LocalDate checkOut, int units) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal perNight = room.getPricePerNight() != null ? room.getPricePerNight() : BigDecimal.ZERO;
        BigDecimal discount = room.getDiscount() != null ? room.getDiscount() : BigDecimal.ZERO;
        BigDecimal effectivePerNight = perNight.subtract(discount).max(BigDecimal.ZERO);
        return effectivePerNight.multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(units));
    }
}
