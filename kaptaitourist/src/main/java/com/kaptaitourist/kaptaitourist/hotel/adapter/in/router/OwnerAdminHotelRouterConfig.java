package com.kaptaitourist.kaptaitourist.hotel.adapter.in.router;

import com.kaptaitourist.kaptaitourist.core.routes.RouteNames;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.handler.HotelHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

@Configuration
@RequiredArgsConstructor
public class OwnerAdminHotelRouterConfig {
    private final HotelHandler hotelHandler;

    // /api/v1/owner/hotel  (HOTEL_OWNER/ADMIN — caller's own hotels)
    private static final String OWNER_HOTELS = RouteNames.BASE_URL.concat(RouteNames.OWNER).concat(RouteNames.HOTEL);
    // /api/v1/admin/hotel  (ADMIN — all hotels + owners + counts)
    private static final String ADMIN_HOTELS = RouteNames.BASE_URL.concat(RouteNames.ADMIN).concat(RouteNames.HOTEL);

    @Bean
    public RouterFunction<ServerResponse> ownerAdminHotelRoutes() {
        return RouterFunctions.route()
                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), builder -> builder
                        .GET(OWNER_HOTELS, hotelHandler::getOwnerHotels)
                        .GET(ADMIN_HOTELS, hotelHandler::getAdminHotels)
                )
                .build();
    }
}
