package backend.dto;

import backend.entity.Frequency;
import backend.entity.PrescriptionStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PrescriptionResponse(
        Long id,
        Long patientId,
        String patientName,
        String doctorName,
        String medicineName,
        String dosage,
        Frequency frequency,
        List<LocalTime> administrationTimes,
        LocalDate startDate,
        LocalDate endDate,
        String instructions,
        String notes,
        PrescriptionStatus status
) {
}