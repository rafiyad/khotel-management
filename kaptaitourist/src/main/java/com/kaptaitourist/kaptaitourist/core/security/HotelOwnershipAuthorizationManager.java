package com.kaptaitourist.kaptaitourist.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Allows the request when the authenticated user is an ADMIN, or is a HOTEL_OWNER who owns
 * the hotel identified by the {hotelId} path variable. Used for hotel-scoped writes.
 */
@Component
@RequiredArgsConstructor
public class HotelOwnershipAuthorizationManager
        implements ReactiveAuthorizationManager<AuthorizationContext> {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_HOTEL_OWNER = "ROLE_HOTEL_OWNER";

    private final OwnershipChecker ownershipChecker;

    @Override
    public Mono<AuthorizationResult> authorize(Mono<Authentication> authentication, AuthorizationContext context) {
        Object hotelIdVar = context.getVariables().get("hotelId");
        String hotelId = hotelIdVar == null ? null : hotelIdVar.toString();

        return authentication
                .filter(Authentication::isAuthenticated)
                .flatMap(auth -> {
                    if (hasAuthority(auth, ROLE_ADMIN)) {
                        return Mono.<AuthorizationResult>just(new AuthorizationDecision(true));
                    }
                    if (!hasAuthority(auth, ROLE_HOTEL_OWNER) || hotelId == null) {
                        return Mono.<AuthorizationResult>just(new AuthorizationDecision(false));
                    }
                    return ownershipChecker.ownsHotel(auth.getName(), hotelId)
                            .map(owns -> (AuthorizationResult) new AuthorizationDecision(owns));
                })
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
