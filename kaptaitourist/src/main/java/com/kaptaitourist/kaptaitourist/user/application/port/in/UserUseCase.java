package com.kaptaitourist.kaptaitourist.user.application.port.in;

import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.AuthResponseDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.LoginRequestDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.ProfileResponseDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.RegisterRequestDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.UserListResponseDto;
import com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto.UserResponseDto;
import reactor.core.publisher.Mono;

public interface UserUseCase {

    Mono<UserResponseDto> register(RegisterRequestDto dto);

    Mono<AuthResponseDto> login(LoginRequestDto dto);

    Mono<UserResponseDto> getCurrentUser(String userId);

    Mono<ProfileResponseDto> getProfile(String userId);

    Mono<UserListResponseDto> findAll();

    Mono<UserResponseDto> promoteToHotelOwner(String userId);
}
