package com.kaptaitourist.kaptaitourist.dashboard.adapter.in.web.router;

import com.kaptaitourist.kaptaitourist.core.routes.RouteNames;
import com.kaptaitourist.kaptaitourist.dashboard.adapter.in.web.handler.DashboardHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

@Configuration
@RequiredArgsConstructor
public class DashboardRouterConfig {
    private final DashboardHandler dashboardHandler;

    // /api/v1/hotel/{hotelId}/dashboard  (owner of the hotel, or admin)
    private static final String HOTEL_DASHBOARD =
            RouteNames.BASE_URL.concat(RouteNames.HOTEL).concat(RouteNames.HOTEL_ID).concat(RouteNames.DASHBOARD);
    // /api/v1/admin/dashboard  (admin only — platform-wide)
    private static final String ADMIN_DASHBOARD =
            RouteNames.BASE_URL.concat(RouteNames.ADMIN).concat(RouteNames.DASHBOARD);

    @Bean
    public RouterFunction<ServerResponse> dashboardRoutes() {
        return RouterFunctions.route()
                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), builder -> builder
                        .GET(HOTEL_DASHBOARD, dashboardHandler::getDashboard)
                        .GET(ADMIN_DASHBOARD, dashboardHandler::getAdminDashboard)
                )
                .build();
    }
}
