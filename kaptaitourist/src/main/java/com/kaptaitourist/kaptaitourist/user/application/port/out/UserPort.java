package com.kaptaitourist.kaptaitourist.user.application.port.out;

import com.kaptaitourist.kaptaitourist.user.domain.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserPort {
    Mono<User> save(User user);                         // create/update; password hash carried on the domain
    Mono<Boolean> existsByEmail(String email);
    Mono<Boolean> existsByMobile(String mobile);
    Mono<User> findByEmail(String email);               // populated with roles + passwordHash (for login)
    Mono<User> findByMobile(String mobile);             // populated with roles + passwordHash (for login)
    Mono<User> findById(String id);                     // populated with roles
    Flux<User> findAll();                               // populated with roles
    Mono<Void> assignRole(String userId, String roleName); // idempotent; errors if role name unknown
    Mono<Void> deleteById(String id);                       // hard delete; used to compensate a failed mirror create
}
