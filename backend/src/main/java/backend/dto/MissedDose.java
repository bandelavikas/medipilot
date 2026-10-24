package backend.dto;

import java.time.LocalDateTime;

public record MissedDose(
        String medicineName,
        String dosage,
        LocalDateTime scheduledAt
) {
}