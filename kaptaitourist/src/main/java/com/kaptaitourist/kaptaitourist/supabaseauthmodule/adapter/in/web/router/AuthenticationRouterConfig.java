package com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.in.web.router;

import com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.in.web.handler.AuthenticationRouterHandler;
import com.kaptaitourist.kaptaitourist.core.routes.RouteNames;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class AuthenticationRouterConfig {
    private final AuthenticationRouterHandler handler;

    private static final String SUPABASE_BASE_URL = RouteNames.BASE_URL.concat(RouteNames.SUPABASE).concat(RouteNames.AUTH);

    @Bean
    public RouterFunction<ServerResponse> authenticationRouter() {
        return RouterFunctions.route()
                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), builder -> builder
                        .POST(SUPABASE_BASE_URL.concat(RouteNames.REGISTER), handler::register)
                        .POST(SUPABASE_BASE_URL.concat(RouteNames.LOGIN), handler::login)
                        .POST(SUPABASE_BASE_URL.concat(RouteNames.FORGOT_PASSWORD), handler::forgotPassword)
                        .POST(SUPABASE_BASE_URL.concat(RouteNames.RESET_PASSWORD), handler::resetPassword)
                )
                .build();
    }
}
