package backend.dto;

import backend.entity.Frequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreatePrescriptionRequest(
        @NotNull Long patientId,
        @NotBlank @Size(max = 150) String medicineName,
        @NotBlank @Size(max = 100) String dosage,
        @NotNull Frequency frequency,
        @NotEmpty List<@NotNull LocalTime> administrationTimes,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 500) String instructions,
        @Size(max = 1000) String notes
) {
}