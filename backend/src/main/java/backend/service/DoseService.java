package backend.service;

import backend.dto.ScheduledDoseResponse;
import backend.entity.DoseStatus;
import backend.entity.MedicationSchedule;
import backend.entity.PatientProfile;
import backend.repository.MedicationScheduleRepository;
import backend.repository.PatientProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DoseService {

    private static final long EARLY_WINDOW_MINUTES = 60;
    private static final long MAX_RANGE_DAYS = 62;

    private final MedicationScheduleRepository scheduleRepository;
    private final PatientProfileRepository patientProfileRepository;

    public DoseService(MedicationScheduleRepository scheduleRepository,
                       PatientProfileRepository patientProfileRepository) {
        this.scheduleRepository = scheduleRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduledDoseResponse> getSchedule(Long patientUserId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw badRequest("The end date cannot be before the start date");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw badRequest("Please ask for " + MAX_RANGE_DAYS + " days or fewer at a time");
        }

        PatientProfile patient = findPatient(patientUserId);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        return scheduleRepository.findForPatientBetween(patient.getId(), start, end)
                .stream()
                .map(dose -> toResponse(dose, now))
                .toList();
    }

    @Transactional
    public ScheduledDoseResponse respond(Long patientUserId, Long doseId, DoseStatus newStatus) {
        PatientProfile patient = findPatient(patientUserId);

        MedicationSchedule dose = scheduleRepository.findById(doseId)
                .filter(found -> found.getPrescription().getPatient().getId().equals(patient.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dose not found"));

        if (newStatus == DoseStatus.PENDING) {
            throw badRequest("Status must be TAKEN or MISSED");
        }
        if (dose.getStatus() != DoseStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This dose has already been recorded");
        }

        LocalDateTime now = LocalDateTime.now();
        if (newStatus == DoseStatus.TAKEN && !canMarkTaken(dose, now)) {
            throw badRequest("This dose cannot be marked as taken yet");
        }
        if (newStatus == DoseStatus.MISSED && !canMarkMissed(dose, now)) {
            throw badRequest("A dose can only be marked as missed after its scheduled time");
        }

        dose.setStatus(newStatus);
        dose.setRespondedAt(now);
        scheduleRepository.save(dose);
        return toResponse(dose, now);
    }

    private boolean canMarkTaken(MedicationSchedule dose, LocalDateTime now) {
        return dose.getStatus() == DoseStatus.PENDING
                && !now.isBefore(dose.getScheduledAt().minusMinutes(EARLY_WINDOW_MINUTES));
    }

    private boolean canMarkMissed(MedicationSchedule dose, LocalDateTime now) {
        return dose.getStatus() == DoseStatus.PENDING
                && !now.isBefore(dose.getScheduledAt());
    }

    private PatientProfile findPatient(Long patientUserId) {
        return patientProfileRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Patient profile not found"));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ScheduledDoseResponse toResponse(MedicationSchedule dose, LocalDateTime now) {
        return new ScheduledDoseResponse(
                dose.getId(),
                dose.getPrescription().getId(),
                dose.getPrescription().getMedicineName(),
                dose.getPrescription().getDosage(),
                dose.getPrescription().getInstructions(),
                dose.getScheduledAt(),
                dose.getStatus(),
                dose.getRespondedAt(),
                canMarkTaken(dose, now),
                canMarkMissed(dose, now));
    }
}