package com.kaptaitourist.kaptaitourist.Room.adapter.in.web.router;

import com.kaptaitourist.kaptaitourist.Room.adapter.in.web.handler.RoomHandler;
import com.kaptaitourist.kaptaitourist.core.routes.RouteNames;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

@Configuration
@RequiredArgsConstructor
public class RoomRouterConfig {
    private final RoomHandler roomHandler;

    // Nested under a hotel: /api/v1/hotel/{hotelId}/room ...
    private static final String ROOM_BASE =
            RouteNames.BASE_URL.concat(RouteNames.HOTEL).concat(RouteNames.HOTEL_ID).concat(RouteNames.ROOM);

    @Bean
    public RouterFunction<ServerResponse> roomRoutes() {
        return RouterFunctions.route()
                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), builder -> builder
                        .POST(ROOM_BASE, roomHandler::createRoom)
                        .GET(ROOM_BASE, roomHandler::getAllRooms)
                        .GET(ROOM_BASE.concat(RouteNames.ROOM_ID), roomHandler::getRoomById)
                        .PUT(ROOM_BASE.concat(RouteNames.ROOM_ID), roomHandler::updateRoom)
                        .DELETE(ROOM_BASE.concat(RouteNames.ROOM_ID), roomHandler::deleteRoom)
                )
                .build();
    }
}
