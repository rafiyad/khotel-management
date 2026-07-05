package com.kaptaitourist.kaptaitourist.user.adapter.out.persistence;

import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.entity.UserEntity;
import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.entity.UserRoleEntity;
import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.repository.RoleRepository;
import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.repository.UserRepository;
import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.repository.UserRoleRepository;
import com.kaptaitourist.kaptaitourist.user.application.port.out.UserPort;
import com.kaptaitourist.kaptaitourist.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserAdapter implements UserPort {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ModelMapper modelMapper;

    @Override
    public Mono<User> save(User user) {
        return userRepository.save(modelMapper.map(user, UserEntity.class))
                .map(entity -> modelMapper.map(entity, User.class))
                .doOnError(e -> log.error("Error saving user: {}", e.getMessage()));
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Mono<Boolean> existsByMobile(String mobile) {
        return userRepository.existsByMobile(mobile);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(entity -> modelMapper.map(entity, User.class))
                .flatMap(this::withRoles);
    }

    @Override
    public Mono<User> findByMobile(String mobile) {
        return userRepository.findByMobile(mobile)
                .map(entity -> modelMapper.map(entity, User.class))
                .flatMap(this::withRoles);
    }

    @Override
    public Mono<User> findById(String id) {
        return userRepository.findById(id)
                .map(entity -> modelMapper.map(entity, User.class))
                .flatMap(this::withRoles);
    }

    @Override
    public Flux<User> findAll() {
        return userRepository.findAllOrdered()
                .map(entity -> modelMapper.map(entity, User.class))
                .flatMap(this::withRoles);
    }

    @Override
    public Mono<Void> assignRole(String userId, String roleName) {
        return roleRepository.findByName(roleName)
                .switchIfEmpty(Mono.error(new ValidationException("Unknown role: " + roleName)))
                .flatMap(role -> userRoleRepository.findByUserIdAndRoleId(userId, role.getId())
                        .hasElement()
                        .flatMap(exists -> exists
                                ? Mono.empty()
                                : userRoleRepository.save(UserRoleEntity.builder()
                                        .userId(userId)
                                        .roleId(role.getId())
                                        .createdAt(LocalDateTime.now())
                                        .build())
                                .then()));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return userRepository.deleteById(id)
                .doOnError(e -> log.error("Error deleting user {}: {}", id, e.getMessage()));
    }

    private Mono<User> withRoles(User user) {
        return userRoleRepository.findRoleNamesByUserId(user.getId())
                .collectList()
                .map(roles -> {
                    user.setRoles(roles);
                    return user;
                });
    }
}
