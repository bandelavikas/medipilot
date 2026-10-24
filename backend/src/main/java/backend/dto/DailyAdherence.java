package backend.dto;

import java.time.LocalDate;

public record DailyAdherence(
        LocalDate date,
        int taken,
        int missed,
        int awaitingResponse,
        int upcoming
) {
}