package com.kaptaitourist.kaptaitourist.welcome.adapter.in.web.handler;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

/** Public smoke-test endpoint: no auth, no DB. */
@Component
public class WelcomeHandler {

    public Mono<ServerResponse> welcome(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("message", "Welcome to the Kaptai Tourist API", "status", "UP"));
    }
}
