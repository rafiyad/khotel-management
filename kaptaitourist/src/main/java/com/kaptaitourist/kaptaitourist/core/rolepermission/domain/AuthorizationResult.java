package com.kaptaitourist.kaptaitourist.core.rolepermission.domain;

/**
 * Outcome of an RBAC check. When {@code granted} and {@code requiresOwnership} are both true, the
 * caller (unless ADMIN) must additionally own the resource identified by {@code urlTemplate}'s
 * {hotelId} variable — checked by the RbacFilter against the actual request path.
 */
public record AuthorizationResult(boolean granted, boolean requiresOwnership, String urlTemplate) {

    public static AuthorizationResult denied() {
        return new AuthorizationResult(false, false, null);
    }

    public static AuthorizationResult granted(boolean requiresOwnership, String urlTemplate) {
        return new AuthorizationResult(true, requiresOwnership, urlTemplate);
    }
}
