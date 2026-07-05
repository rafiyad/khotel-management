package com.kaptaitourist.kaptaitourist.ownerrequest.application.port.out;

import com.kaptaitourist.kaptaitourist.ownerrequest.domain.OwnerRequest;
import com.kaptaitourist.kaptaitourist.ownerrequest.domain.OwnerRequestView;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface OwnerRequestPort {
    Mono<OwnerRequest> save(OwnerRequest request);
    Mono<OwnerRequest> findById(String id);
    /** Requests joined with requester identity, newest first; {@code status} null = all. */
    Flux<OwnerRequestView> listViews(String status);
    Mono<OwnerRequest> updateDecision(String id, String status, String adminId, LocalDateTime decidedAt);
}
