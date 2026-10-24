package backend.security;

import backend.entity.Role;

public record AuthenticatedUser(
        Long id,
        String email,
        String fullName,
        Role role
) {
}