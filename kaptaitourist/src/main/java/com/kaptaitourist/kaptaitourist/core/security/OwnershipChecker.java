package com.kaptaitourist.kaptaitourist.core.security;

import reactor.core.publisher.Mono;

/**
 * Resolves whether a user owns/manages a given hotel — the resource-level check layered on top of
 * role-based RBAC. Implemented in the hotel module (dependency inversion: hotel depends on core).
 */
public interface OwnershipChecker {
    Mono<Boolean> ownsHotel(String userId, String hotelId);
}
