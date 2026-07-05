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
public class ImageRouterConfig {
    private final ImageHandler imageHandler;

    @Bean
    public RouterFunction<ServerResponse> routerConfig () {
        return RouterFunctions.route()
                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), builder -> builder
                        .POST(RouteNames.BASE_URL.concat(RouteNames.IMAGE).concat(RouteNames.SAVE).concat(RouteNames.HOTEL_ID), imageHandler::saveImage)
                        .GET(RouteNames.BASE_URL.concat(RouteNames.IMAGE).concat(RouteNames.HOTEL_ID), imageHandler::getAllByHotelId)
                        .GET(RouteNames.BASE_URL.concat(RouteNames.IMAGE).concat(RouteNames.HOTEL_ID).concat(RouteNames.FIND_BY_IMAGE_ID), imageHandler::getImageById)
                        .PUT(RouteNames.BASE_URL.concat(RouteNames.IMAGE).concat(RouteNames.HOTEL_ID).concat(RouteNames.FIND_BY_IMAGE_ID), imageHandler::updateImage)
                        .DELETE(RouteNames.BASE_URL.concat(RouteNames.IMAGE).concat(RouteNames.HOTEL_ID).concat(RouteNames.FIND_BY_IMAGE_ID), imageHandler::deleteImage)
                        .DELETE(RouteNames.BASE_URL.concat(RouteNames.IMAGE).concat(RouteNames.HOTEL_ID), imageHandler::deleteAllByHotelId)
                        .POST(RouteNames.BASE_URL.concat(RouteNames.IMAGE).concat(RouteNames.HOTEL_ID).concat(RouteNames.FIND_BY_IMAGE_ID).concat(RouteNames.PRIMARY), imageHandler::setHotelImagePrimary)
                )
                .build();
    }
}
