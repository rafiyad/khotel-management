package com.kaptaitourist.kaptaitourist.welcome.adapter.in.web.router;

import com.kaptaitourist.kaptaitourist.core.routes.RouteNames;
import com.kaptaitourist.kaptaitourist.welcome.adapter.in.web.handler.WelcomeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class WelcomeRouterConfig {
    private final WelcomeHandler welcomeHandler;

    @Bean
    public RouterFunction<ServerResponse> welcomeRoutes() {
        return RouterFunctions.route()
                .GET(RouteNames.BASE_URL.concat("/welcome"), welcomeHandler::welcome)
                .build();
    }
}
