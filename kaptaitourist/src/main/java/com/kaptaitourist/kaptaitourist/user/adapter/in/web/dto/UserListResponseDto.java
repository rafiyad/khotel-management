package com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserListResponseDto {
    private String message;
    private int totalRecords;
    private List<User> userData;
}
