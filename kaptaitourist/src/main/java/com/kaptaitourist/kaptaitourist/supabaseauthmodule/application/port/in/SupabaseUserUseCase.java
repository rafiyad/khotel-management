package com.kaptaitourist.kaptaitourist.supabaseauthmodule.application.port.in;

import com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.in.web.dto.*;
import reactor.core.publisher.Mono;

public interface SupabaseUserUseCase {

    /** Mirror-creates the user in both Supabase Auth and khotel_user. Returns the user, no token. */
    Mono<UserResponseDto> register(RegisterRequestDto dto);

    /** Verifies credentials against Supabase and returns the Supabase access token + local roles. */
    Mono<AuthResponseDto> login(LoginRequestDto dto);

    /** Triggers Supabase's recovery email. Always succeeds (enumeration-safe). */
    Mono<MessageResponse> forgotPassword(ForgotPasswordRequestDto dto);

    /** Completes the reset using the emailed OTP code (verify + set new password). */
    Mono<MessageResponse> resetPassword(ResetPasswordRequestDto dto);
}
