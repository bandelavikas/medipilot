package backend.dto;

import java.time.LocalDate;

public record PatientSummary(
        Long patientId,
        Long userId,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth
) {
}