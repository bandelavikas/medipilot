package backend.service;

import backend.dto.CancelPrescriptionResponse;
import backend.entity.DoseStatus;
import backend.entity.Prescription;
import backend.entity.PrescriptionStatus;
import backend.repository.MedicationScheduleRepository;
import backend.repository.PrescriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class PrescriptionLifecycleService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicationScheduleRepository scheduleRepository;

    public PrescriptionLifecycleService(PrescriptionRepository prescriptionRepository,
                                        MedicationScheduleRepository scheduleRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public CancelPrescriptionResponse cancel(Long doctorUserId, Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .filter(found -> found.getDoctor().getUser().getId().equals(doctorUserId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prescription not found"));

        if (prescription.getStatus() != PrescriptionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only active prescriptions can be cancelled");
        }

        prescription.setStatus(PrescriptionStatus.CANCELLED);
        prescriptionRepository.save(prescription);

        // Remove only doses that are still in the future and unanswered.
        // Doses that were answered, or whose time has already passed, stay in the history.
        int removed = scheduleRepository.deleteFuturePending(
                prescriptionId, DoseStatus.PENDING, LocalDateTime.now());

        return new CancelPrescriptionResponse(prescriptionId, removed);
    }

    @Transactional
    public int completeFinished() {
        return prescriptionRepository.markCompleted(
                PrescriptionStatus.ACTIVE, PrescriptionStatus.COMPLETED, LocalDate.now());
    }
}