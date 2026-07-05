package com.kaptaitourist.kaptaitourist.user.adapter.in.web.router;

import com.kaptaitourist.kaptaitourist.core.routes.RouteNames;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.handler.AuthHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

@Configuration
@RequiredArgsConstructor
public class AuthRouterConfig {
    private final AuthHandler authHandler;

    private static final String AUTH = RouteNames.BASE_URL.concat(RouteNames.AUTH);   // /api/v1/auth
    private static final String USER = RouteNames.BASE_URL.concat(RouteNames.USER);   // /api/v1/user

    @Bean
    public RouterFunction<ServerResponse> authRoutes() {
        return RouterFunctions.route()
                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), builder -> builder
                        .POST(AUTH.concat("/register"), authHandler::register)
                        .POST(AUTH.concat("/login"), authHandler::login)
                        .GET(AUTH.concat("/me"), authHandler::me)
                        .GET(AUTH.concat(RouteNames.PROFILE), authHandler::profile)
                        .POST(AUTH.concat("/change-password"), authHandler::changePassword)
                        // Admin-only (enforced by SecurityConfig: /api/v1/user/** → ROLE_ADMIN)
                        .GET(USER, authHandler::listUsers)
                        .POST(USER.concat(RouteNames.USER_ID).concat(RouteNames.PROMOTE), authHandler::promote)
                )
                .build();
    }
}
