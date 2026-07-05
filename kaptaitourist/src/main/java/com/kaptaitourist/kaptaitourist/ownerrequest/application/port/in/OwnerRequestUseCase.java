package com.kaptaitourist.kaptaitourist.ownerrequest.application.port.in;

import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto.OwnerRequestListResponseDto;
import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto.OwnerRequestResponseDto;
import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto.RegisterOwnerRequestDto;
import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto.RegisterOwnerResponseDto;
import reactor.core.publisher.Mono;

public interface OwnerRequestUseCase {
    /** Public: register a USER and file a PENDING enlistment request (HOTEL_OWNER not granted yet). */
    Mono<RegisterOwnerResponseDto> registerOwner(RegisterOwnerRequestDto dto);

    Mono<OwnerRequestListResponseDto> list(String status);

    /** Admin: grant HOTEL_OWNER to the requester and mark the request APPROVED (409 if already decided). */
    Mono<OwnerRequestResponseDto> approve(String requestId, String adminId);

    /** Admin: mark the request REJECTED (409 if already decided). */
    Mono<OwnerRequestResponseDto> reject(String requestId, String adminId);
}
