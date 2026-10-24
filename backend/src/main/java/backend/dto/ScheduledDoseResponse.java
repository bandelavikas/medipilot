package backend.dto;

import backend.entity.DoseStatus;

import java.time.LocalDateTime;

public record ScheduledDoseResponse(
        Long id,
        Long prescriptionId,
        String medicineName,
        String dosage,
        String instructions,
        LocalDateTime scheduledAt,
        DoseStatus status,
        LocalDateTime respondedAt,
        boolean canMarkTaken,
        boolean canMarkMissed
) {
}