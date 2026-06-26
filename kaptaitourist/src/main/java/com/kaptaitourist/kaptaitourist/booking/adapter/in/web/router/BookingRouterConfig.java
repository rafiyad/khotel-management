package com.kaptaitourist.kaptaitourist.booking.adapter.in.web.router;

import com.kaptaitourist.kaptaitourist.booking.adapter.in.web.handler.BookingHandler;
import com.kaptaitourist.kaptaitourist.core.routes.RouteNames;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

@Configuration
@RequiredArgsConstructor
public class BookingRouterConfig {
    private final BookingHandler bookingHandler;

    private static final String HOTEL_ROOM =
            RouteNames.BASE_URL.concat(RouteNames.HOTEL).concat(RouteNames.HOTEL_ID)
                    .concat(RouteNames.ROOM).concat(RouteNames.ROOM_ID);
    // /api/v1/hotel/{hotelId}/room/{roomId}/booking and .../availability
    private static final String ROOM_BOOKING = HOTEL_ROOM.concat(RouteNames.BOOKING);
    private static final String ROOM_AVAILABILITY = HOTEL_ROOM.concat(RouteNames.AVAILABILITY);
    // /api/v1/hotel/{hotelId}/booking ...
    private static final String HOTEL_BOOKING =
            RouteNames.BASE_URL.concat(RouteNames.HOTEL).concat(RouteNames.HOTEL_ID).concat(RouteNames.BOOKING);

    @Bean
    public RouterFunction<ServerResponse> bookingRoutes() {
        return RouterFunctions.route()
                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), builder -> builder
                        .GET(ROOM_AVAILABILITY, bookingHandler::checkAvailability)
                        .POST(ROOM_BOOKING, bookingHandler::createBooking)
                        .GET(HOTEL_BOOKING, bookingHandler::getHotelBookings)
                        .GET(HOTEL_BOOKING.concat(RouteNames.BOOKING_ID), bookingHandler::getBookingById)
                        .POST(HOTEL_BOOKING.concat(RouteNames.BOOKING_ID).concat(RouteNames.CANCEL), bookingHandler::cancelBooking)
                )
                .build();
    }
}
