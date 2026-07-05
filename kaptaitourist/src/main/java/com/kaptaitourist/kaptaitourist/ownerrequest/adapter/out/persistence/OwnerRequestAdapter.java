package com.kaptaitourist.kaptaitourist.ownerrequest.adapter.out.persistence;

import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.out.persistence.entity.OwnerRequestEntity;
import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.out.persistence.repository.OwnerRequestRepository;
import com.kaptaitourist.kaptaitourist.ownerrequest.application.port.out.OwnerRequestPort;
import com.kaptaitourist.kaptaitourist.ownerrequest.domain.OwnerRequest;
import com.kaptaitourist.kaptaitourist.ownerrequest.domain.OwnerRequestView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class OwnerRequestAdapter implements OwnerRequestPort {

    private final OwnerRequestRepository repository;
    private final DatabaseClient db;
    private final ModelMapper modelMapper;

    private static final String LIST_SQL = """
            SELECT r.id, r.user_id, u.name AS requester_name, u.email AS requester_email,
                   r.hotel_name, r.message, r.status, r.decided_by, r.decided_at, r.created_at
            FROM khotel_owner_request r JOIN khotel_user u ON u.id = r.user_id
            """;

    @Override
    public Mono<OwnerRequest> save(OwnerRequest request) {
        return repository.save(modelMapper.map(request, OwnerRequestEntity.class))
                .map(entity -> modelMapper.map(entity, OwnerRequest.class))
                .doOnError(e -> log.error("Error saving owner request: {}", e.getMessage()));
    }

    @Override
    public Mono<OwnerRequest> findById(String id) {
        return repository.findById(id)
                .map(entity -> modelMapper.map(entity, OwnerRequest.class));
    }

    @Override
    public Flux<OwnerRequestView> listViews(String status) {
        String sql = LIST_SQL + (status != null ? " WHERE r.status = :status" : "") + " ORDER BY r.created_at DESC";
        DatabaseClient.GenericExecuteSpec spec = db.sql(sql);
        if (status != null) {
            spec = spec.bind("status", status);
        }
        return spec.map((row, md) -> OwnerRequestView.builder()
                        .id(row.get("id", String.class))
                        .userId(row.get("user_id", String.class))
                        .requesterName(row.get("requester_name", String.class))
                        .requesterEmail(row.get("requester_email", String.class))
                        .hotelName(row.get("hotel_name", String.class))
                        .message(row.get("message", String.class))
                        .status(row.get("status", String.class))
                        .decidedBy(row.get("decided_by", String.class))
                        .decidedAt(row.get("decided_at", LocalDateTime.class))
                        .createdAt(row.get("created_at", LocalDateTime.class))
                        .build())
                .all()
                .doOnError(e -> log.error("Error listing owner requests: {}", e.getMessage()));
    }

    @Override
    public Mono<OwnerRequest> updateDecision(String id, String status, String adminId, LocalDateTime decidedAt) {
        return repository.findById(id)
                .flatMap(entity -> {
                    entity.setStatus(status);
                    entity.setDecidedBy(adminId);
                    entity.setDecidedAt(decidedAt);
                    entity.setUpdatedAt(decidedAt);
                    return repository.save(entity);
                })
                .map(entity -> modelMapper.map(entity, OwnerRequest.class));
    }
}
