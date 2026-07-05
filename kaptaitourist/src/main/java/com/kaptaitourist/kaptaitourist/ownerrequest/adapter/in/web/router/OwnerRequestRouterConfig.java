package com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.router;

import com.kaptaitourist.kaptaitourist.core.routes.RouteNames;
import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.handler.OwnerRequestHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

@Configuration
@RequiredArgsConstructor
public class OwnerRequestRouterConfig {
    private final OwnerRequestHandler ownerRequestHandler;

    // POST /api/v1/auth/register-owner   (public)
    private static final String REGISTER_OWNER = RouteNames.BASE_URL.concat(RouteNames.AUTH).concat("/register-owner");
    // /api/v1/owner-request ...          (admin)
    private static final String OWNER_REQUEST = RouteNames.BASE_URL.concat(RouteNames.OWNER_REQUEST);

    @Bean
    public RouterFunction<ServerResponse> ownerRequestRoutes() {
        return RouterFunctions.route()
                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), builder -> builder
                        .POST(REGISTER_OWNER, ownerRequestHandler::registerOwner)
                        .GET(OWNER_REQUEST, ownerRequestHandler::list)
                        .POST(OWNER_REQUEST.concat(RouteNames.REQUEST_ID).concat(RouteNames.APPROVE), ownerRequestHandler::approve)
                        .POST(OWNER_REQUEST.concat(RouteNames.REQUEST_ID).concat(RouteNames.REJECT), ownerRequestHandler::reject)
                )
                .build();
    }
}
