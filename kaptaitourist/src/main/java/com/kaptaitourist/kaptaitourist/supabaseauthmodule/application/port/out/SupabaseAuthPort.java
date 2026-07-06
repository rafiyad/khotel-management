package com.kaptaitourist.kaptaitourist.supabaseauthmodule.application.port.out;

import com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.out.supabase.dto.SupabaseSession;
import com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.out.supabase.dto.SupabaseUser;
import reactor.core.publisher.Mono;

import java.util.Map;

/** Outbound port to the Supabase Auth (GoTrue) REST API — see doc/supabase-auth.md §3. */
public interface SupabaseAuthPort {

    /** POST /auth/v1/signup — creates the Supabase user; {@code metadata} becomes user_metadata. */
    Mono<SupabaseUser> signUp(String email, String password, Map<String, Object> metadata);

    /** POST /auth/v1/token?grant_type=password — verifies credentials and returns a session. */
    Mono<SupabaseSession> signInWithPassword(String email, String password);

    /** POST /auth/v1/recover — sends the reset email; always succeeds (enumeration-safe). */
    Mono<Void> recover(String email);

    /** POST /auth/v1/verify (type=recovery) — exchanges the emailed OTP for a session. */
    Mono<SupabaseSession> verifyRecoveryOtp(String email, String token);

    /** PUT /auth/v1/user — sets a new password using a bearer access token. */
    Mono<Void> updatePassword(String accessToken, String newPassword);
}
