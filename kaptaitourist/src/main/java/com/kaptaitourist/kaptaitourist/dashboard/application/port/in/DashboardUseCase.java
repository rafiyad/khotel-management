package com.kaptaitourist.kaptaitourist.dashboard.application.port.in;

import com.kaptaitourist.kaptaitourist.dashboard.adapter.in.web.dto.AdminDashboardResponseDto;
import com.kaptaitourist.kaptaitourist.dashboard.adapter.in.web.dto.DashboardResponseDto;
import reactor.core.publisher.Mono;

public interface DashboardUseCase {
    /** Builds the dashboard for a hotel (404 if the hotel does not exist). */
    Mono<DashboardResponseDto> getDashboard(String hotelId);

    /** Builds the platform-wide dashboard across all hotels (admin only). */
    Mono<AdminDashboardResponseDto> getAdminDashboard();
}
