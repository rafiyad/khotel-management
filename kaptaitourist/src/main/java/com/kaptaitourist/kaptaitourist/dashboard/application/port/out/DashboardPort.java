package com.kaptaitourist.kaptaitourist.dashboard.application.port.out;

import com.kaptaitourist.kaptaitourist.dashboard.domain.DashboardSnapshot;
import com.kaptaitourist.kaptaitourist.dashboard.domain.PlatformSnapshot;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface DashboardPort {
    /** Computes the live aggregate snapshot for a hotel as of {@code today}. */
    Mono<DashboardSnapshot> load(String hotelId, LocalDate today);

    /** Computes the live platform-wide snapshot (all hotels) as of {@code today}. */
    Mono<PlatformSnapshot> loadPlatform(LocalDate today);
}
