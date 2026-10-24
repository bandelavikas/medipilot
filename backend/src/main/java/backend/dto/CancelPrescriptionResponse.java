package backend.dto;

public record CancelPrescriptionResponse(
        Long prescriptionId,
        int futureDosesRemoved
) {
}