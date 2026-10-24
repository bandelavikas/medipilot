package backend.service;

import backend.entity.MedicationSchedule;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ScheduleService {

    private final MedicationScheduleRepository scheduleRepository;
    private final PrescriptionRepository prescriptionRepository;

    public ScheduleService(MedicationScheduleRepository scheduleRepository,
                           PrescriptionRepository prescriptionRepository) {
        this.scheduleRepository = scheduleRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    // Creates the dose rows for a prescription. Safe to run more than once:
    // times that already exist are skipped, so no duplicates are ever created.
    @Transactional
    public int generateForPrescription(Prescription prescription) {
        if (prescription.getStatus() != PrescriptionStatus.ACTIVE) {
            return 0;
        }

        Set<LocalDateTime> alreadyScheduled =
                new HashSet<>(scheduleRepository.findScheduledTimes(prescription.getId()));
        LocalDateTime now = LocalDateTime.now();
        List<LocalTime> doseTimes = prescription.getAdministrationTimes();

        List<MedicationSchedule> newDoses = new ArrayList<>();
        LocalDate date = prescription.getStartDate();
        while (!date.isAfter(prescription.getEndDate())) {
            for (LocalTime time : doseTimes) {
                LocalDateTime scheduledAt = LocalDateTime.of(date, time);

                // Skip doses whose time has already passed, and doses that already exist.
                if (scheduledAt.isBefore(now) || alreadyScheduled.contains(scheduledAt)) {
                    continue;
                }

                MedicationSchedule dose = new MedicationSchedule();
                dose.setPrescription(prescription);
                dose.setScheduledAt(scheduledAt);
                newDoses.add(dose);
            }
            date = date.plusDays(1);
        }

        scheduleRepository.saveAll(newDoses);
        return newDoses.size();
    }

    @Transactional
    public int generateForDoctorPrescription(Long doctorUserId, Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .filter(found -> found.getDoctor().getUser().getId().equals(doctorUserId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prescription not found"));
        return generateForPrescription(prescription);
    }
}