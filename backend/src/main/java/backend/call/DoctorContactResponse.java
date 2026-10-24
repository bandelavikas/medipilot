package backend.call;

public record DoctorContactResponse(
        Long userId,
        String fullName,
        String specialization
) {
}