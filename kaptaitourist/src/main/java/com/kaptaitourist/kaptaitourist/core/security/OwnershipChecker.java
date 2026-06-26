package com.kaptaitourist.kaptaitourist.core.security;

import reactor.core.publisher.Mono;

/** Resolves whether a user owns/manages a given hotel (for resource-level authorization). */
public interface OwnershipChecker {
    Mono<Boolean> ownsHotel(String userId, String hotelId);
}
