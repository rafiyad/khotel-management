package com.kaptaitourist.kaptaitourist.user.adapter.out.persistence;

import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.entity.UserEntity;
import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.repository.RoleRepository;
import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.repository.UserRepository;
import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.repository.UserRoleRepository;
import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.entity.UserRoleEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Ensures a bootstrap ADMIN exists on startup (admins can't self-register).
 * Idempotent: skips if a user with the configured admin email already exists.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;
    @Value("${app.admin.name}")
    private String adminName;
    @Value("${app.admin.mobile}")
    private String adminMobile;

    @Override
    public void run(ApplicationArguments args) {
        userRepository.existsByEmail(adminEmail.toLowerCase())
                .flatMap(exists -> exists ? Mono.empty() : createAdmin())
                .doOnError(e -> log.error("Admin seeding failed: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .block();
    }

    private Mono<Void> createAdmin() {
        return roleRepository.findByName(ADMIN_ROLE)
                .switchIfEmpty(Mono.error(new IllegalStateException("ADMIN role not seeded")))
                .flatMap(role -> userRepository.save(UserEntity.builder()
                                .name(adminName)
                                .email(adminEmail.toLowerCase())
                                .mobile(adminMobile)
                                .passwordHash(passwordEncoder.encode(adminPassword))
                                .isActive(true)
                                .createdBy("system")
                                .createdAt(LocalDateTime.now())
                                .build())
                        .flatMap(saved -> userRoleRepository.save(UserRoleEntity.builder()
                                .userId(saved.getId())
                                .roleId(role.getId())
                                .createdAt(LocalDateTime.now())
                                .build())))
                .doOnSuccess(v -> log.info("Bootstrap ADMIN created: {}", adminEmail))
                .then();
    }
}
