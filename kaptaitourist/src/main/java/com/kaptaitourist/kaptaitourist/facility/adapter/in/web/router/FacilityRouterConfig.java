package com.kaptaitourist.kaptaitourist.facility.adapter.in.web.router;

import com.kaptaitourist.kaptaitourist.core.routes.RouteNames;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.handler.FacilityHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

@Configuration
@RequiredArgsConstructor
public class FacilityRouterConfig {
    private final FacilityHandler facilityHandler;

    // Catalog: /api/v1/facility ...
    private static final String CATALOG = RouteNames.BASE_URL.concat(RouteNames.FACILITY);
    // Hotel assignment: /api/v1/hotel/{hotelId}/facility ...
    private static final String HOTEL_FACILITY =
            RouteNames.BASE_URL.concat(RouteNames.HOTEL).concat(RouteNames.HOTEL_ID).concat(RouteNames.FACILITY);
    // Room assignment: /api/v1/hotel/{hotelId}/room/{roomId}/facility ...
    private static final String ROOM_FACILITY =
            RouteNames.BASE_URL.concat(RouteNames.HOTEL).concat(RouteNames.HOTEL_ID)
                    .concat(RouteNames.ROOM).concat(RouteNames.ROOM_ID).concat(RouteNames.FACILITY);

    @Bean
    public RouterFunction<ServerResponse> facilityRoutes() {
        return RouterFunctions.route()
                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), builder -> builder
                        // Catalog
                        .POST(CATALOG, facilityHandler::createFacility)
                        .GET(CATALOG, facilityHandler::getAllFacilities)
                        .GET(CATALOG.concat(RouteNames.FACILITY_ID), facilityHandler::getFacilityById)
                        .PUT(CATALOG.concat(RouteNames.FACILITY_ID), facilityHandler::updateFacility)
                        .DELETE(CATALOG.concat(RouteNames.FACILITY_ID), facilityHandler::deleteFacility)
                        // Hotel assignment
                        .POST(HOTEL_FACILITY, facilityHandler::assignToHotel)
                        .GET(HOTEL_FACILITY, facilityHandler::getHotelFacilities)
                        .DELETE(HOTEL_FACILITY.concat(RouteNames.FACILITY_ID), facilityHandler::unassignFromHotel)
                        // Room assignment
                        .POST(ROOM_FACILITY, facilityHandler::assignToRoom)
                        .GET(ROOM_FACILITY, facilityHandler::getRoomFacilities)
                        .DELETE(ROOM_FACILITY.concat(RouteNames.FACILITY_ID), facilityHandler::unassignFromRoom)
                )
                .build();
    }
}
