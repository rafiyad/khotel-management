package com.kaptaitourist.kaptaitourist.dashboard.application.service;

import com.kaptaitourist.kaptaitourist.core.exception.HotelNotFoundException;
import com.kaptaitourist.kaptaitourist.dashboard.adapter.in.web.dto.AdminDashboardResponseDto;
import com.kaptaitourist.kaptaitourist.dashboard.adapter.in.web.dto.DashboardResponseDto;
import com.kaptaitourist.kaptaitourist.dashboard.application.port.in.DashboardUseCase;
import com.kaptaitourist.kaptaitourist.dashboard.application.port.out.DashboardPort;
import com.kaptaitourist.kaptaitourist.dashboard.domain.DashboardSnapshot;
import com.kaptaitourist.kaptaitourist.dashboard.domain.PlatformSnapshot;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService implements DashboardUseCase {

    private final DashboardPort dashboardPort;
    private final HotelPort hotelPort;

    @Override
    public Mono<DashboardResponseDto> getDashboard(String hotelId) {
        LocalDate today = LocalDate.now();
        return hotelPort.existsById(hotelId)
                .flatMap(exists -> exists
                        ? dashboardPort.load(hotelId, today)
                        : Mono.error(new HotelNotFoundException("Hotel not found with id: " + hotelId)))
                .map(snapshot -> toDto(hotelId, today, snapshot))
                .doOnError(e -> log.error("Error building dashboard for hotel {}: {}", hotelId, e.getMessage()));
    }

    @Override
    public Mono<AdminDashboardResponseDto> getAdminDashboard() {
        LocalDate today = LocalDate.now();
        return dashboardPort.loadPlatform(today)
                .map(s -> toAdminDto(today, s))
                .doOnError(e -> log.error("Error building admin dashboard: {}", e.getMessage()));
    }

    private AdminDashboardResponseDto toAdminDto(LocalDate today, PlatformSnapshot s) {
        return AdminDashboardResponseDto.builder()
                .message("Admin dashboard retrieved successfully")
                .asOf(today)
                .platform(AdminDashboardResponseDto.Platform.builder()
                        .hotels(s.getHotels())
                        .owners(s.getOwners())
                        .users(s.getUsers())
                        .facilities(s.getFacilities())
                        .build())
                .rooms(AdminDashboardResponseDto.Rooms.builder()
                        .roomTypes(s.getRoomTypes())
                        .totalUnits(s.getTotalUnits())
                        .bookedTonight(s.getBookedTonight())
                        .availableTonight(s.getAvailableTonight())
                        .occupancyPct(s.getOccupancyPct())
                        .build())
                .bookings(AdminDashboardResponseDto.Bookings.builder()
                        .upcoming(s.getUpcomingBookings())
                        .arrivalsToday(s.getArrivalsToday())
                        .departuresToday(s.getDeparturesToday())
                        .cancelled(s.getCancelledBookings())
                        .build())
                .revenue(AdminDashboardResponseDto.Revenue.builder()
                        .upcoming(s.getUpcomingRevenue())
                        .build())
                .build();
    }

    private DashboardResponseDto toDto(String hotelId, LocalDate today, DashboardSnapshot s) {
        return DashboardResponseDto.builder()
                .message("Dashboard retrieved successfully")
                .hotelId(hotelId)
                .asOf(today)
                .rooms(DashboardResponseDto.Rooms.builder()
                        .roomTypes(s.getRoomTypes())
                        .totalUnits(s.getTotalUnits())
                        .bookedTonight(s.getBookedTonight())
                        .availableTonight(s.getAvailableTonight())
                        .occupancyPct(s.getOccupancyPct())
                        .build())
                .bookings(DashboardResponseDto.Bookings.builder()
                        .upcoming(s.getUpcomingBookings())
                        .arrivalsToday(s.getArrivalsToday())
                        .departuresToday(s.getDeparturesToday())
                        .cancelled(s.getCancelledBookings())
                        .build())
                .revenue(DashboardResponseDto.Revenue.builder()
                        .upcoming(s.getUpcomingRevenue())
                        .build())
                .facilities(DashboardResponseDto.Facilities.builder()
                        .assigned(s.getFacilitiesAssigned())
                        .unavailable(s.getFacilitiesUnavailable())
                        .build())
                .media(DashboardResponseDto.Media.builder()
                        .hotelImages(s.getHotelImages())
                        .build())
                .build();
    }
}
