package com.kaptaitourist.kaptaitourist.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final HotelOwnershipAuthorizationManager ownership;

    /** Comma-separated list of allowed browser origins (e.g. the Next.js dev server). */
    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         CorsConfigurationSource corsConfigurationSource) {
        AuthenticationWebFilter jwtFilter =
                new AuthenticationWebFilter(new JwtReactiveAuthenticationManager(jwtService));
        jwtFilter.setServerAuthenticationConverter(new JwtServerAuthenticationConverter());

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        // CORS preflight must always be allowed through
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // ── Auth ──────────────────────────────────────────────
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .pathMatchers("/api/v1/auth/me", "/api/v1/auth/profile").authenticated()
                        .pathMatchers("/api/v1/user/**").hasRole("ADMIN")

                        // ── Facility catalog: reads owner/admin (hidden from USER), writes admin ──
                        .pathMatchers(HttpMethod.GET, "/api/v1/facility/**").hasAnyRole("HOTEL_OWNER", "ADMIN")
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

                        // ── Facilities & images under a hotel: owner/admin only (hidden from USER), all methods ──
                        .pathMatchers("/api/v1/hotel/{hotelId}/room/{roomId}/image/**").access(ownership)
                        .pathMatchers("/api/v1/hotel/{hotelId}/room/{roomId}/facility/**").access(ownership)
                        .pathMatchers("/api/v1/hotel/{hotelId}/facility/**").access(ownership)

                        // ── Browsing rooms / hotel detail is public; other writes are owner/admin ──
                        .pathMatchers(HttpMethod.GET, "/api/v1/hotel/{hotelId}/**").permitAll()
                        .pathMatchers("/api/v1/hotel/{hotelId}/**").access(ownership)

                        // ── Hotel-level images (flat /image route): owner/admin only (hidden from USER) ──
                        .pathMatchers("/api/v1/image/save/{hotelId}").access(ownership)
                        .pathMatchers("/api/v1/image/{hotelId}/{imageId}").access(ownership)
                        .pathMatchers("/api/v1/image/{hotelId}").access(ownership)

                        // Anything else (e.g. PUT/DELETE /hotel/{hotelId} handled above) stays open
                        .anyExchange().permitAll())
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
