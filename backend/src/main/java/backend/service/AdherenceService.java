package backend.service;

import backend.dto.AdherenceReport;
import backend.dto.DailyAdherence;
import backend.dto.MedicineAdherence;
import backend.dto.MissedDose;
import backend.entity.DoctorProfile;
import backend.entity.MedicationSchedule;
import backend.entity.PatientProfile;
import backend.repository.DoctorProfileRepository;
import backend.repository.MedicationScheduleRepository;
import backend.repository.PatientProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdherenceService {

    private static final long MAX_RANGE_DAYS = 92;
    private static final int RECENT_MISSED_LIMIT = 10;

    // Positions inside the small counting arrays used below.
    private static final int TAKEN = 0;
    private static final int MISSED = 1;
    private static final int AWAITING = 2;
    private static final int UPCOMING = 3;

    private final MedicationScheduleRepository scheduleRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    public AdherenceService(MedicationScheduleRepository scheduleRepository,
                            PatientProfileRepository patientProfileRepository,
                            DoctorProfileRepository doctorProfileRepository) {
        this.scheduleRepository = scheduleRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    @Transactional(readOnly = true)
    public AdherenceReport reportForPatient(Long patientUserId, LocalDate from, LocalDate to) {
        validateRange(from, to);

        PatientProfile patient = patientProfileRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Patient profile not found"));

        List<MedicationSchedule> doses = loadDoses(patient.getId(), from, to);
        return buildReport(doses, from, to);
    }

    @Transactional(readOnly = true)
    public AdherenceReport reportForDoctor(Long doctorUserId, Long patientId,
                                           LocalDate from, LocalDate to) {
        validateRange(from, to);

        DoctorProfile doctor = doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Doctor profile not found"));

        PatientProfile patient = patientProfileRepository.findById(patientId)
                .filter(found -> found.getAssignedDoctor() != null
                        && found.getAssignedDoctor().getId().equals(doctor.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Patient not found in your list"));

        List<MedicationSchedule> doses = loadDoses(patient.getId(), from, to).stream()
                .filter(dose -> dose.getPrescription().getDoctor().getId().equals(doctor.getId()))
                .toList();
        return buildReport(doses, from, to);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The end date cannot be before the start date");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please ask for " + MAX_RANGE_DAYS + " days or fewer at a time");
        }
    }

    private List<MedicationSchedule> loadDoses(Long patientId, LocalDate from, LocalDate to) {
        return scheduleRepository.findForPatientBetween(
                patientId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private AdherenceReport buildReport(List<MedicationSchedule> doses, LocalDate from, LocalDate to) {
        LocalDateTime now = LocalDateTime.now();

        int[] totals = new int[4];

        Map<LocalDate, int[]> daily = new LinkedHashMap<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            daily.put(date, new int[4]);
        }

        Map<String, int[]> byMedicine = new LinkedHashMap<>();

        Map<String, Integer> missedByTimeOfDay = new LinkedHashMap<>();
        missedByTimeOfDay.put("MORNING", 0);
        missedByTimeOfDay.put("AFTERNOON", 0);
        missedByTimeOfDay.put("EVENING", 0);
        missedByTimeOfDay.put("NIGHT", 0);

        List<MedicationSchedule> missedDoses = new ArrayList<>();

        for (MedicationSchedule dose : doses) {
            int slot = slotFor(dose, now);

            totals[slot]++;
            daily.get(dose.getScheduledAt().toLocalDate())[slot]++;
            byMedicine.computeIfAbsent(dose.getPrescription().getMedicineName(), key -> new int[4])[slot]++;

            if (slot == MISSED) {
                missedDoses.add(dose);
                missedByTimeOfDay.merge(partOfDay(dose.getScheduledAt().getHour()), 1, Integer::sum);
            }
        }

        List<DailyAdherence> dailyList = daily.entrySet().stream()
                .map(entry -> new DailyAdherence(
                        entry.getKey(),
                        entry.getValue()[TAKEN],
                        entry.getValue()[MISSED],
                        entry.getValue()[AWAITING],
                        entry.getValue()[UPCOMING]))
                .toList();

        List<MedicineAdherence> medicineList = byMedicine.entrySet().stream()
                .map(entry -> new MedicineAdherence(
                        entry.getKey(),
                        entry.getValue()[TAKEN],
                        entry.getValue()[MISSED],
                        entry.getValue()[AWAITING],
                        percent(entry.getValue()[TAKEN], entry.getValue()[MISSED])))
                .toList();

        List<MissedDose> recentMissed = missedDoses.stream()
                .sorted(Comparator.comparing(MedicationSchedule::getScheduledAt).reversed())
                .limit(RECENT_MISSED_LIMIT)
                .map(dose -> new MissedDose(
                        dose.getPrescription().getMedicineName(),
                        dose.getPrescription().getDosage(),
                        dose.getScheduledAt()))
                .toList();

        return new AdherenceReport(
                from,
                to,
                totals[TAKEN],
                totals[MISSED],
                totals[AWAITING],
                totals[UPCOMING],
                percent(totals[TAKEN], totals[MISSED]),
                dailyList,
                medicineList,
                recentMissed,
                missedByTimeOfDay);
    }

    // Taken and Missed come from the patient's own answer.
    // A pending dose is "awaiting response" once its time has passed, otherwise "upcoming".
    private int slotFor(MedicationSchedule dose, LocalDateTime now) {
        return switch (dose.getStatus()) {
            case TAKEN -> TAKEN;
            case MISSED -> MISSED;
            case PENDING -> dose.getScheduledAt().isAfter(now) ? UPCOMING : AWAITING;
        };
    }

    // Adherence = taken / (taken + missed). Returns null when there is no answered dose yet.
    private Double percent(int taken, int missed) {
        int answered = taken + missed;
        if (answered == 0) {
            return null;
        }
        return Math.round(taken * 1000.0 / answered) / 10.0;
    }

    private String partOfDay(int hour) {
        if (hour >= 5 && hour < 12) {
            return "MORNING";
        }
        if (hour >= 12 && hour < 17) {
            return "AFTERNOON";
        }
        if (hour >= 17 && hour < 21) {
            return "EVENING";
        }
        return "NIGHT";
    }
}