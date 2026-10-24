package backend.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AdherenceReport(
        LocalDate from,
        LocalDate to,
        int taken,
        int missed,
        int awaitingResponse,
        int upcoming,
        Double adherencePercent,
        List<DailyAdherence> daily,
        List<MedicineAdherence> byMedicine,
        List<MissedDose> recentMissed,
        Map<String, Integer> missedByTimeOfDay
) {
}