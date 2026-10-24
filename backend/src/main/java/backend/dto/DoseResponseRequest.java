package backend.dto;

import backend.entity.DoseStatus;
import jakarta.validation.constraints.NotNull;

public record DoseResponseRequest(
        @NotNull DoseStatus status
) {
}