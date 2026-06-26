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
public class HotelRouterConfig {
    private final HotelHandler hotelHandler;

    @Bean
    public RouterFunction<ServerResponse> hotelRoutes() {
        return RouterFunctions.route()
                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), builder -> builder
                        .POST(RouteNames.BASE_URL.concat(RouteNames.HOTEL), hotelHandler::createHotel)
                        .GET(RouteNames.BASE_URL.concat(RouteNames.HOTEL), hotelHandler::getAllHotels)
                        .GET(RouteNames.BASE_URL.concat(RouteNames.HOTEL).concat(RouteNames.HOTEL_ID), hotelHandler::getHotelById)
                        .PUT(RouteNames.BASE_URL.concat(RouteNames.HOTEL).concat(RouteNames.HOTEL_ID), hotelHandler::updateHotel)
                        .DELETE(RouteNames.BASE_URL.concat(RouteNames.HOTEL).concat(RouteNames.HOTEL_ID), hotelHandler::deleteHotel)
                )
                .build();
    }
}
