package backend.dto;

public record MedicineAdherence(
        String medicineName,
        int taken,
        int missed,
        int awaitingResponse,
        Double adherencePercent
) {
}