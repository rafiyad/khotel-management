package com.kaptaitourist.kaptaitourist.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final HotelOwnershipAuthorizationManager ownership;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter jwtFilter =
                new AuthenticationWebFilter(new JwtReactiveAuthenticationManager(jwtService));
        jwtFilter.setServerAuthenticationConverter(new JwtServerAuthenticationConverter());

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        // ── Auth ──────────────────────────────────────────────
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .pathMatchers("/api/v1/auth/me").authenticated()
                        .pathMatchers("/api/v1/user/**").hasRole("ADMIN")

                        // ── Facility catalog: reads public, writes admin ───────
                        .pathMatchers(HttpMethod.GET, "/api/v1/facility/**").permitAll()
                        .pathMatchers("/api/v1/facility/**").hasRole("ADMIN")

                        // ── Hotel collection ───────────────────────────────────
                        .pathMatchers(HttpMethod.GET, "/api/v1/hotel").permitAll()              // browse hotels
                        .pathMatchers(HttpMethod.POST, "/api/v1/hotel").hasAnyRole("HOTEL_OWNER", "ADMIN")

                        // ── Hotel resource (own row): read public, update/delete owner/admin ──
                        .pathMatchers(HttpMethod.GET, "/api/v1/hotel/{hotelId}").permitAll()
                        .pathMatchers(HttpMethod.PUT, "/api/v1/hotel/{hotelId}").access(ownership)
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/hotel/{hotelId}").access(ownership)

                        // ── Booking: availability public, creating requires login ──
                        .pathMatchers(HttpMethod.GET, "/api/v1/hotel/{hotelId}/room/{roomId}/availability").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/hotel/{hotelId}/room/{roomId}/booking").authenticated()
                        // booking management (list/get/cancel) → owner or admin
                        .pathMatchers("/api/v1/hotel/{hotelId}/booking/**").access(ownership)

                        // ── Browsing a hotel (rooms/images/facilities) is public; writes are owner/admin ──
                        .pathMatchers(HttpMethod.GET, "/api/v1/hotel/{hotelId}/**").permitAll()
                        .pathMatchers("/api/v1/hotel/{hotelId}/**").access(ownership)

                        // ── Hotel-level images (flat /image route): reads public, writes owner/admin ──
                        .pathMatchers(HttpMethod.GET, "/api/v1/image/**").permitAll()
                        .pathMatchers("/api/v1/image/save/{hotelId}").access(ownership)
                        .pathMatchers("/api/v1/image/{hotelId}/{imageId}").access(ownership)
                        .pathMatchers("/api/v1/image/{hotelId}").access(ownership)

                        // Anything else (e.g. PUT/DELETE /hotel/{hotelId} handled above) stays open
                        .anyExchange().permitAll())
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
