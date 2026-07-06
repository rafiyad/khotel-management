package com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.out.supabase;

import com.kaptaitourist.kaptaitourist.core.exception.ConflictException;
import com.kaptaitourist.kaptaitourist.core.exception.InvalidCredentialsException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.out.supabase.dto.SupabaseSession;
import com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.out.supabase.dto.SupabaseUser;
import com.kaptaitourist.kaptaitourist.supabaseauthmodule.application.port.out.SupabaseAuthPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Reactive proxy to Supabase Auth (GoTrue). All calls carry the anon key as {@code apikey};
 * that key alone is enough for register / login / recover / reset (see doc/supabase-auth.md §3).
 */
@Slf4j
@Component
public class SupabaseAuthClient implements SupabaseAuthPort {

    private final WebClient webClient;

    public SupabaseAuthClient(@Value("${supabase.url}") String supabaseUrl,
                              @Value("${supabase.anon-key}") String anonKey) {
        this.webClient = WebClient.builder()
                .baseUrl(supabaseUrl + "/auth/v1")
                .defaultHeader("apikey", anonKey)
                .defaultHeader("Authorization", "Bearer " + anonKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Mono<SupabaseUser> signUp(String email, String password, Map<String, Object> metadata) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("data", metadata);   // stored as user_metadata

        return webClient.post()
                .uri("/signup")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class).defaultIfEmpty("").flatMap(err -> {
                            log.warn("Supabase signup failed ({}): {}", response.statusCode(), err);
                            String lower = err.toLowerCase();
                            if (lower.contains("already") || lower.contains("exists") || lower.contains("registered"))
                                return Mono.error(new ConflictException("Email is already registered"));
                            return Mono.error(new RuntimeException("Supabase signup failed: " + err));
                        }))
                .bodyToMono(SupabaseUser.class);
    }

    @Override
    public Mono<SupabaseSession> signInWithPassword(String email, String password) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/token").queryParam("grant_type", "password").build())
                .bodyValue(Map.of("email", email, "password", password))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class).defaultIfEmpty("").flatMap(err -> {
                            log.warn("Supabase login failed ({}): {}", response.statusCode(), err);
                            if (err.toLowerCase().contains("not confirmed"))
                                return Mono.error(new InvalidCredentialsException(
                                        "Please confirm your email before logging in."));
                            return Mono.error(new InvalidCredentialsException("Invalid email or password"));
                        }))
                .bodyToMono(SupabaseSession.class);
    }

    @Override
    public Mono<Void> recover(String email) {
        return webClient.post()
                .uri("/recover")
                .bodyValue(Map.of("email", email))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class).defaultIfEmpty("").flatMap(err -> {
                            log.warn("Supabase recover failed ({}): {}", response.statusCode(), err);
                            return Mono.error(new RuntimeException("Could not send the reset email. Please try again later."));
                        }))
                .bodyToMono(Void.class);
    }

    @Override
    public Mono<SupabaseSession> verifyRecoveryOtp(String email, String token) {
        return webClient.post()
                .uri("/verify")
                .bodyValue(Map.of("type", "recovery", "email", email, "token", token))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class).defaultIfEmpty("").flatMap(err -> {
                            log.warn("Supabase verify failed ({}): {}", response.statusCode(), err);
                            return Mono.error(new ValidationException("Reset code is invalid or has expired"));
                        }))
                .bodyToMono(SupabaseSession.class);
    }

    @Override
    public Mono<Void> updatePassword(String accessToken, String newPassword) {
        return webClient.put()
                .uri("/user")
                .headers(h -> h.set("Authorization", "Bearer " + accessToken))
                .bodyValue(Map.of("password", newPassword))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class).defaultIfEmpty("").flatMap(err -> {
                            log.warn("Supabase password update failed ({}): {}", response.statusCode(), err);
                            return Mono.error(new RuntimeException("Could not update the password: " + err));
                        }))
                .bodyToMono(Void.class);
    }
}
