package com.kaptaitourist.kaptaitourist.ownerrequest.application.service;

import com.kaptaitourist.kaptaitourist.core.exception.ConflictException;
import com.kaptaitourist.kaptaitourist.core.exception.OwnerRequestNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto.OwnerRequestListResponseDto;
import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto.OwnerRequestResponseDto;
import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto.RegisterOwnerRequestDto;
import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto.RegisterOwnerResponseDto;
import com.kaptaitourist.kaptaitourist.ownerrequest.application.port.in.OwnerRequestUseCase;
import com.kaptaitourist.kaptaitourist.ownerrequest.application.port.out.OwnerRequestPort;
import com.kaptaitourist.kaptaitourist.ownerrequest.domain.OwnerRequest;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.RegisterRequestDto;
import com.kaptaitourist.kaptaitourist.user.application.port.in.UserUseCase;
import com.kaptaitourist.kaptaitourist.user.application.port.out.UserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OwnerRequestService implements OwnerRequestUseCase {

    private static final String ROLE_HOTEL_OWNER = "HOTEL_OWNER";
    private static final String PENDING = "PENDING";
    private static final List<String> STATUSES = List.of("PENDING", "APPROVED", "REJECTED");

    private final OwnerRequestPort ownerRequestPort;
    private final UserUseCase userUseCase;
    private final UserPort userPort;

    // ----------------------------------- Register owner -----------------------------------

    @Override
    public Mono<RegisterOwnerResponseDto> registerOwner(RegisterOwnerRequestDto dto) {
        if (dto.getHotelName() == null || dto.getHotelName().isBlank())
            return Mono.error(new ValidationException("hotelName is required"));

        RegisterRequestDto reg = RegisterRequestDto.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .mobile(dto.getMobile())
                .gender(dto.getGender())
                .password(dto.getPassword())
                .build();

        // Create the USER (validates + hashes + assigns USER role); then file the PENDING request.
        return userUseCase.register(reg)
                .flatMap(userResp -> ownerRequestPort.save(OwnerRequest.builder()
                                .userId(userResp.getUserData().getId())
                                .hotelName(dto.getHotelName())
                                .message(dto.getMessage())
                                .status(PENDING)
                                .createdAt(LocalDateTime.now())
                                .build())
                        .map(saved -> RegisterOwnerResponseDto.builder()
                                .message("Owner registration submitted; awaiting admin approval")
                                .userData(userResp.getUserData())
                                .requestData(saved)
                                .build()))
                .doOnSuccess(r -> log.info("Owner enlistment filed for {}", dto.getEmail()));
    }

    // ----------------------------------- List ---------------------------------------------

    @Override
    public Mono<OwnerRequestListResponseDto> list(String status) {
        String filter = (status != null && STATUSES.contains(status.toUpperCase())) ? status.toUpperCase() : null;
        return ownerRequestPort.listViews(filter)
                .collectList()
                .map(list -> OwnerRequestListResponseDto.builder()
                        .message("Owner requests retrieved successfully")
                        .totalRecords(list.size())
                        .requestData(list)
                        .build());
    }

    // ----------------------------------- Approve / Reject ---------------------------------

    @Override
    @Transactional
    public Mono<OwnerRequestResponseDto> approve(String requestId, String adminId) {
        return getPending(requestId)
                .flatMap(req -> userPort.assignRole(req.getUserId(), ROLE_HOTEL_OWNER)
                        .then(ownerRequestPort.updateDecision(requestId, "APPROVED", adminId, LocalDateTime.now())))
                .map(updated -> OwnerRequestResponseDto.builder()
                        .message("Owner request approved")
                        .requestData(updated)
                        .build())
                .doOnSuccess(r -> log.info("Owner request {} approved by {}", requestId, adminId));
    }

    @Override
    @Transactional
    public Mono<OwnerRequestResponseDto> reject(String requestId, String adminId) {
        return getPending(requestId)
                .flatMap(req -> ownerRequestPort.updateDecision(requestId, "REJECTED", adminId, LocalDateTime.now()))
                .map(updated -> OwnerRequestResponseDto.builder()
                        .message("Owner request rejected")
                        .requestData(updated)
                        .build())
                .doOnSuccess(r -> log.info("Owner request {} rejected by {}", requestId, adminId));
    }

    /** Loads a request, 404 if missing, 409 if already decided (terminal). */
    private Mono<OwnerRequest> getPending(String requestId) {
        return ownerRequestPort.findById(requestId)
                .switchIfEmpty(Mono.error(new OwnerRequestNotFoundException("Owner request not found: " + requestId)))
                .flatMap(req -> PENDING.equals(req.getStatus())
                        ? Mono.just(req)
                        : Mono.error(new ConflictException(
                                "Request has already been " + req.getStatus().toLowerCase())));
    }
}
