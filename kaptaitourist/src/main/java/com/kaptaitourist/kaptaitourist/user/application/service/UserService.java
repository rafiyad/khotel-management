package com.kaptaitourist.kaptaitourist.user.application.service;

import com.kaptaitourist.kaptaitourist.core.exception.*;
import com.kaptaitourist.kaptaitourist.core.security.JwtService;
import com.kaptaitourist.kaptaitourist.core.util.MaskUtil;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.AuthResponseDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.ChangePasswordRequestDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.ProfileDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.ProfileResponseDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.LoginRequestDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.RegisterRequestDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.UserListResponseDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.UserResponseDto;
import com.kaptaitourist.kaptaitourist.user.application.port.in.UserUseCase;
import com.kaptaitourist.kaptaitourist.user.application.port.out.UserPort;
import com.kaptaitourist.kaptaitourist.user.domain.Gender;
import com.kaptaitourist.kaptaitourist.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_HOTEL_OWNER = "HOTEL_OWNER";
    private static final String MOBILE_PATTERN = "^\\+?[0-9]{6,20}$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final UserPort userPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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

        String email = dto.getEmail().trim().toLowerCase();
        String mobile = dto.getMobile().trim();
        String gender = dto.getGender().trim().toUpperCase();
        if (!Gender.isValid(gender))
            return Mono.error(new ValidationException("gender must be MALE or FEMALE"));

        return userPort.existsByEmail(email)
                .flatMap(emailTaken -> {
                    if (emailTaken)
                        return Mono.<User>error(new ConflictException("Email is already registered"));
                    return userPort.existsByMobile(mobile).flatMap(mobileTaken -> {
                        if (mobileTaken)
                            return Mono.<User>error(new ConflictException("Mobile number is already registered"));
                        return userPort.save(User.builder()
                                .name(dto.getName())
                                .email(email)
                                .mobile(mobile)
                                .gender(gender)
                                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                                .isActive(true)
                                .createdBy("self-registration")
                                .createdAt(LocalDateTime.now())
                                .build());
                    });
                })
                .flatMap(saved -> userPort.assignRole(saved.getId(), ROLE_USER)
                        .then(userPort.findById(saved.getId())))
                .map(user -> UserResponseDto.builder()
                        .message("Registration successful")
                        .userData(withoutSecret(user))
                        .build())
                .doOnSuccess(r -> log.info("Registered user: {}", r.getUserData().getEmail()))
                .doOnError(e -> log.error("Error registering user: {}", e.getMessage()));
    }

    // ----------------------------------- Login --------------------------------------------

    @Override
    public Mono<AuthResponseDto> login(LoginRequestDto dto) {
        boolean hasEmail = dto.getEmail() != null && !dto.getEmail().isBlank();
        boolean hasMobile = dto.getMobile() != null && !dto.getMobile().isBlank();
        if ((!hasEmail && !hasMobile) || dto.getPassword() == null)
            return Mono.error(new ValidationException("email or mobile, and password, are required"));

        // Look up by whichever identifier was supplied (email takes precedence).
        Mono<com.kaptaitourist.kaptaitourist.user.domain.User> found = hasEmail
                ? userPort.findByEmail(dto.getEmail().trim().toLowerCase())
                : userPort.findByMobile(dto.getMobile().trim());

        return found
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid email/mobile or password")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash()))
                        return Mono.error(new InvalidCredentialsException("Invalid email or password"));
                    if (user.getIsActive() != null && !user.getIsActive())
                        return Mono.error(new InvalidCredentialsException("Account is disabled"));

                    String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRoles());
                    return Mono.just(AuthResponseDto.builder()
                            .token(token)
                            .tokenType("Bearer")
                            .userId(user.getId())
                            .email(user.getEmail())
                            .roles(user.getRoles())
                            .build());
                })
                .doOnError(e -> log.warn("Login failed for {}: {}",
                        hasEmail ? dto.getEmail() : dto.getMobile(), e.getMessage()));
    }

    // ----------------------------------- Current user -------------------------------------

    @Override
    public Mono<UserResponseDto> getCurrentUser(String userId) {
        return userPort.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with id: " + userId)))
                .map(user -> UserResponseDto.builder()
                        .message("Current user")
                        .userData(withoutSecret(user))
                        .build());
    }

    // ----------------------------------- Profile (masked) ---------------------------------

    @Override
    public Mono<ProfileResponseDto> getProfile(String userId) {
        return userPort.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with id: " + userId)))
                .map(user -> ProfileResponseDto.builder()
                        .message("Profile")
                        .profileData(ProfileDto.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .email(MaskUtil.maskEmail(user.getEmail()))
                                .mobile(MaskUtil.maskMobile(user.getMobile()))
                                .gender(user.getGender())
                                .isActive(user.getIsActive())
                                .roles(user.getRoles())
                                .build())
                        .build());
    }

    // ----------------------------------- Admin: list --------------------------------------

    @Override
    public Mono<UserListResponseDto> findAll() {
        return userPort.findAll()
                .map(this::withoutSecret)
                .collectList()
                .map(list -> UserListResponseDto.builder()
                        .message("Users retrieved successfully")
                        .totalRecords(list.size())
                        .userData(list)
                        .build());
    }

    // ----------------------------------- Admin: promote -----------------------------------

    @Override
    public Mono<UserResponseDto> promoteToHotelOwner(String userId) {
        return userPort.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with id: " + userId)))
                .flatMap(user -> userPort.assignRole(userId, ROLE_HOTEL_OWNER)
                        .then(userPort.findById(userId)))
                .map(user -> UserResponseDto.builder()
                        .message("User promoted to HOTEL_OWNER")
                        .userData(withoutSecret(user))
                        .build())
                .doOnSuccess(r -> log.info("Promoted user {} to HOTEL_OWNER", userId));
    }

    // ----------------------------------- Change password ----------------------------------

    @Override
    public Mono<Void> changePassword(String userId, ChangePasswordRequestDto dto) {
        if (dto.getCurrentPassword() == null || dto.getCurrentPassword().isBlank())
            return Mono.error(new ValidationException("currentPassword is required"));
        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 6)
            return Mono.error(new ValidationException("newPassword must be at least 6 characters"));
        if (dto.getNewPassword().equals(dto.getCurrentPassword()))
            return Mono.error(new ValidationException("newPassword must be different from the current password"));

        return userPort.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with id: " + userId)))
                .flatMap(user -> {
                    // Verify the caller actually knows the current password before changing it.
                    if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash()))
                        return Mono.error(new InvalidCredentialsException("Current password is incorrect"));
                    user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
                    user.setUpdatedBy(userId);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userPort.save(user);
                })
                .doOnSuccess(u -> log.info("Password changed for user {}", userId))
                .doOnError(e -> log.warn("Password change failed for {}: {}", userId, e.getMessage()))
                .then();
    }

    // ----------------------------------- Helpers ------------------------------------------

    /**
     * Clears the password hash before a domain User is handed to the web layer. Guarantees the
     * hash never serializes regardless of which Jackson (2 vs 3 / annotation recognition) the
     * WebFlux codec uses — the {@code @JsonIgnore} on the field is kept as a second line of defence.
     */
    private User withoutSecret(User user) {
        if (user != null) {
            user.setPasswordHash(null);
        }
        return user;
    }
}
