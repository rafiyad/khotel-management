package com.kaptaitourist.kaptaitourist.core.security;

import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextService {

    // TODO: adjust import + type to match your actual user repository
    private final UserRepository userRepository;

    /**
     * Builds "Name_ROLE" (e.g. "John Doe_ADMIN") for the currently authenticated user,
     * for use in createdBy/updatedBy audit fields.
     */
    public Mono<String> getCurrentUserLabel() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth != null && auth.isAuthenticated())
                .switchIfEmpty(Mono.error(new IllegalStateException("No authenticated user in security context")))
                .flatMap(auth -> {
                    String userId = String.valueOf(auth.getPrincipal()); // JWT subject, set in JwtReactiveAuthenticationManager
                    String role = extractRole(auth.getAuthorities());

                    return userRepository.findById(userId) // TODO: adjust if your id type isn't String
                            .map(user -> user.getName() + "_" + role) // TODO: adjust getName() to your actual field
                            .switchIfEmpty(Mono.error(new IllegalStateException("User not found for id: " + userId)));
                });
    }

    /** The authenticated caller's id and whether they hold the ADMIN role — for resource-level checks. */
    public Mono<AuthContext> getAuthContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth != null && auth.isAuthenticated())
                .switchIfEmpty(Mono.error(new IllegalStateException("No authenticated user in security context")))
                .map(auth -> {
                    String userId = String.valueOf(auth.getPrincipal());
                    boolean isAdmin = auth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .anyMatch(a -> a.equals("ADMIN") || a.equals("ROLE_ADMIN"));
                    return new AuthContext(userId, isAdmin);
                });
    }

    /** Minimal auth snapshot for resource-level authorization. */
    public record AuthContext(String userId, boolean isAdmin) {
    }

    private String extractRole(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .orElse("UNKNOWN");
    }
}