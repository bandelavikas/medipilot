package backend.dto;

import backend.entity.Role;

public record AuthResponse(
        String token,
        Long userId,
        String fullName,
        String email,
        Role role
) {
}