package backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AssignPatientRequest(
        @NotBlank @Email String email
) {
}