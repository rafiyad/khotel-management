package com.kaptaitourist.kaptaitourist.supabaseauthmodule.application.service;

import com.kaptaitourist.kaptaitourist.core.exception.ConflictException;
import com.kaptaitourist.kaptaitourist.core.exception.InvalidCredentialsException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.in.web.dto.*;
import com.kaptaitourist.kaptaitourist.supabaseauthmodule.application.port.in.SupabaseUserUseCase;
import com.kaptaitourist.kaptaitourist.supabaseauthmodule.application.port.out.SupabaseAuthPort;
import com.kaptaitourist.kaptaitourist.user.application.port.out.UserPort;
import com.kaptaitourist.kaptaitourist.user.domain.Gender;
import com.kaptaitourist.kaptaitourist.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Supabase-backed auth. Registration uses the <b>mirror method</b>: the user is written to
 * both khotel_user and Supabase Auth, linked by email. Atomicity is by ordering — the local
 * row is created first, then Supabase; if Supabase fails the local row is deleted, so a
 * failure on either side leaves nothing behind. No local password is stored (Supabase owns it).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SupabaseAuthService implements SupabaseUserUseCase {

    private static final String ROLE_USER = "USER";
    private static final String MOBILE_PATTERN = "^\\+?[0-9]{6,20}$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final SupabaseAuthPort supabaseAuthPort;
    private final UserPort userPort;

    // ----------------------------------- Register -----------------------------------------

    @Override
    public Mono<UserResponseDto> register(RegisterRequestDto dto) {
        if (dto.getName() == null || dto.getName().isBlank())
            return Mono.error(new ValidationException("name is required"));
        if (dto.getEmail() == null || dto.getEmail().isBlank())
            return Mono.error(new ValidationException("email is required"));
        if (!dto.getEmail().trim().matches(EMAIL_PATTERN))
            return Mono.error(new ValidationException("email is not a valid email address"));
        if (dto.getMobile() == null || dto.getMobile().isBlank())
            return Mono.error(new ValidationException("mobile is required"));
        if (!dto.getMobile().trim().matches(MOBILE_PATTERN))
            return Mono.error(new ValidationException("mobile must be 6–20 digits, optionally prefixed with '+'"));
        if (dto.getPassword() == null || dto.getPassword().length() < 6)
            return Mono.error(new ValidationException("password must be at least 6 characters"));
        if (dto.getGender() == null || dto.getGender().isBlank())
            return Mono.error(new ValidationException("gender is required"));

        String name = dto.getName().trim();
        String email = dto.getEmail().trim().toLowerCase();
        String mobile = dto.getMobile().trim();
        String gender = dto.getGender().trim().toUpperCase();
        if (!Gender.isValid(gender))
            return Mono.error(new ValidationException("gender must be MALE or FEMALE"));

        return userPort.existsByEmail(email)
                .flatMap(emailTaken -> emailTaken
                        ? Mono.<Boolean>error(new ConflictException("Email is already registered"))
                        : userPort.existsByMobile(mobile))
                .flatMap(mobileTaken -> mobileTaken
                        ? Mono.<User>error(new ConflictException("Mobile number is already registered"))
                        // Local row first; Supabase second. If Supabase fails, roll the local row back.
                        : userPort.save(User.builder()
                                .name(name)
                                .email(email)
                                .mobile(mobile)
                                .gender(gender)
                                .passwordHash(null)          // Supabase owns the password
                                .isActive(true)
                                .createdBy("supabase-registration")
                                .createdAt(LocalDateTime.now())
                                .build()))
                .flatMap(saved -> userPort.assignRole(saved.getId(), ROLE_USER)
                        .then(supabaseAuthPort.signUp(email, dto.getPassword(), metadata(name, mobile, gender)))
                        .thenReturn(saved)
                        // Compensate: any failure after the local insert removes the orphan local row.
                        .onErrorResume(err -> userPort.deleteById(saved.getId())
                                .doOnSuccess(v -> log.warn("Rolled back local user {} after Supabase failure", email))
                                .then(Mono.error(err))))
                .flatMap(saved -> userPort.findById(saved.getId()))
                .map(user -> UserResponseDto.builder()
                        .message("Registration successful")
                        .userData(withoutSecret(user))
                        .build())
                .doOnSuccess(r -> log.info("Registered (mirror) user: {}", email))
                .doOnError(e -> log.error("Error registering user {}: {}", email, e.getMessage()));
    }

    // ----------------------------------- Login --------------------------------------------

    @Override
    public Mono<AuthResponseDto> login(LoginRequestDto dto) {
        boolean hasEmail = dto.getEmail() != null && !dto.getEmail().isBlank();
        boolean hasMobile = dto.getMobile() != null && !dto.getMobile().isBlank();
        if ((!hasEmail && !hasMobile) || dto.getPassword() == null || dto.getPassword().isBlank())
            return Mono.error(new ValidationException("email or mobile, and password, are required"));

        // Supabase authenticates by email; resolve email from mobile via the local mirror if needed.
        Mono<String> emailMono = hasEmail
                ? Mono.just(dto.getEmail().trim().toLowerCase())
                : userPort.findByMobile(dto.getMobile().trim())
                        .map(User::getEmail)
                        .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid mobile or password")));

        return emailMono.flatMap(email -> supabaseAuthPort.signInWithPassword(email, dto.getPassword())
                .flatMap(session -> userPort.findByEmail(email)
                        .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid email or password")))
                        .map(user -> AuthResponseDto.builder()
                                .token(session.getAccessToken())
                                .tokenType("Bearer")
                                .userId(user.getId())
                                .email(user.getEmail())
                                .roles(user.getRoles())
                                .build())))
                .doOnSuccess(r -> log.info("Login success (supabase) for {}", r.getEmail()))
                .doOnError(e -> log.warn("Login failed: {}", e.getMessage()));
    }

    // ----------------------------------- Forgot password ----------------------------------

    @Override
    public Mono<MessageResponse> forgotPassword(ForgotPasswordRequestDto dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank())
            return Mono.error(new ValidationException("email is required"));
        if (!dto.getEmail().trim().matches(EMAIL_PATTERN))
            return Mono.error(new ValidationException("email is not a valid email address"));

        return supabaseAuthPort.recover(dto.getEmail().trim().toLowerCase())
                .onErrorResume(ex -> {
                    log.warn("Supabase recover failed for {}: {}", dto.getEmail(), ex.getMessage());
                    return Mono.empty(); // swallow — don't leak infra errors to client
                })
                .thenReturn(MessageResponse.builder()
                        .message("If an account exists for that email, a reset code has been sent.")
                        .build());
    }

    // ----------------------------------- Reset password -----------------------------------

    @Override
    public Mono<MessageResponse> resetPassword(ResetPasswordRequestDto dto) {
        if (dto.getAccessToken() == null || dto.getAccessToken().isBlank())
            return Mono.error(new ValidationException("accessToken is required"));
        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 6)
            return Mono.error(new ValidationException("newPassword must be at least 6 characters"));

        return supabaseAuthPort.updatePassword(dto.getAccessToken(), dto.getNewPassword())
                .thenReturn(MessageResponse.builder()
                        .message("Password has been reset. You can now log in with your new password.")
                        .build())
                .doOnSuccess(r -> log.info("Password reset via magic link"));
    }

    // ----------------------------------- Helpers ------------------------------------------

    private Map<String, Object> metadata(String name, String mobile, String gender) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("mobile", mobile);
        data.put("gender", gender);
        return data;
    }

    /** Never expose the (unused) local password hash column value. */
    private User withoutSecret(User user) {
        if (user != null) user.setPasswordHash(null);
        return user;
    }
}
