package com.kaptaitourist.kaptaitourist.image.adapter.in.web.router;

import com.kaptaitourist.kaptaitourist.core.routes.RouteNames;
import com.kaptaitourist.kaptaitourist.image.adapter.in.web.handler.ImageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

@Configuration
@RequiredArgsConstructor
public class RoomImageRouterConfig {
    private final ImageHandler imageHandler;

    // Nested under a room: /api/v1/hotel/{hotelId}/room/{roomId}/image ...
    private static final String ROOM_IMAGE_BASE = RouteNames.BASE_URL
            .concat(RouteNames.HOTEL).concat(RouteNames.HOTEL_ID)
            .concat(RouteNames.ROOM).concat(RouteNames.ROOM_ID)
            .concat(RouteNames.IMAGE);

    @Bean
    public RouterFunction<ServerResponse> roomImageRoutes() {
        return RouterFunctions.route()
                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), builder -> builder
                        .POST(ROOM_IMAGE_BASE, imageHandler::saveRoomImage)
                        .GET(ROOM_IMAGE_BASE, imageHandler::getRoomImages)
                        .DELETE(ROOM_IMAGE_BASE.concat(RouteNames.FIND_BY_IMAGE_ID), imageHandler::deleteRoomImage)
                        .DELETE(ROOM_IMAGE_BASE, imageHandler::deleteAllRoomImages)
                )
                .build();
    }
}
