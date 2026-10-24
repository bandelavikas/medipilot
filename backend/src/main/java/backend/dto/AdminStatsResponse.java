package backend.dto;

public record AdminStatsResponse(
        long totalUsers,
        long admins,
        long doctors,
        long patients,
        long activeUsers,
        long disabledUsers,
        long totalPrescriptions,
        long activePrescriptions
) {
}