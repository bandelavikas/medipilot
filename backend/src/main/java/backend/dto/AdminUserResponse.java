package backend.dto;

import backend.entity.Role;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean active,
        LocalDateTime createdAt
) {
}