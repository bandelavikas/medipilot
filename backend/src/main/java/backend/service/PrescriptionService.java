package backend.service;

import backend.dto.CreatePrescriptionRequest;
import backend.dto.PrescriptionResponse;
import backend.entity.DoctorProfile;
import backend.entity.PatientProfile;
import backend.entity.Prescription;
import backend.entity.PrescriptionStatus;
import backend.repository.DoctorProfileRepository;
import backend.repository.PatientProfileRepository;
import backend.repository.PrescriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class PrescriptionService {

    private static final long MAX_DURATION_DAYS = 365;

    private final PrescriptionRepository prescriptionRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final ScheduleService scheduleService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               DoctorProfileRepository doctorProfileRepository,
                               PatientProfileRepository patientProfileRepository,
                               ScheduleService scheduleService) {
        this.prescriptionRepository = prescriptionRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.scheduleService = scheduleService;
    }

    @Transactional
    public PrescriptionResponse create(Long doctorUserId, CreatePrescriptionRequest request) {
        DoctorProfile doctor = findDoctor(doctorUserId);
        PatientProfile patient = findOwnPatient(doctor, request.patientId());

        validate(request);

        Prescription prescription = new Prescription();
        prescription.setDoctor(doctor);
        prescription.setPatient(patient);
        prescription.setMedicineName(request.medicineName().trim());
        prescription.setDosage(request.dosage().trim());
        prescription.setFrequency(request.frequency());
        prescription.setAdministrationTimes(new ArrayList<>(new TreeSet<>(request.administrationTimes())));
        prescription.setStartDate(request.startDate());
        prescription.setEndDate(request.endDate());
        prescription.setInstructions(blankToNull(request.instructions()));
        prescription.setNotes(blankToNull(request.notes()));
        prescription.setStatus(PrescriptionStatus.ACTIVE);

        prescriptionRepository.save(prescription);
        scheduleService.generateForPrescription(prescription);
        return toResponse(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> listForDoctorPatient(Long doctorUserId, Long patientId) {
        DoctorProfile doctor = findDoctor(doctorUserId);
        PatientProfile patient = findOwnPatient(doctor, patientId);

        return prescriptionRepository.findByPatientId(patient.getId())
                .stream()
                .filter(prescription -> prescription.getDoctor().getId().equals(doctor.getId()))
                .sorted(Comparator.comparing(Prescription::getStartDate).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> listForPatient(Long patientUserId) {
        PatientProfile patient = patientProfileRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Patient profile not found"));

        return prescriptionRepository.findByPatientId(patient.getId())
                .stream()
                .sorted(Comparator.comparing(Prescription::getStartDate).reversed())
                .map(this::toResponse)
                .toList();
    }

    private void validate(CreatePrescriptionRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw badRequest("End date cannot be before the start date");
        }
        if (request.endDate().isBefore(LocalDate.now())) {
            throw badRequest("End date cannot be in the past");
        }
        if (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > MAX_DURATION_DAYS) {
            throw badRequest("A prescription cannot last longer than " + MAX_DURATION_DAYS + " days");
        }

        Set<LocalTime> distinctTimes = new TreeSet<>(request.administrationTimes());
        if (distinctTimes.size() != request.administrationTimes().size()) {
            throw badRequest("Dose times must all be different");
        }
        int expected = request.frequency().getDosesPerDay();
        if (distinctTimes.size() != expected) {
            throw badRequest("This frequency needs exactly " + expected + " dose time(s) per day");
        }
    }

    private DoctorProfile findDoctor(Long doctorUserId) {
        return doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Doctor profile not found"));
    }

    private PatientProfile findOwnPatient(DoctorProfile doctor, Long patientId) {
        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(this::patientNotInList);

        if (patient.getAssignedDoctor() == null
                || !patient.getAssignedDoctor().getId().equals(doctor.getId())) {
            throw patientNotInList();
        }
        return patient;
    }

    private ResponseStatusException patientNotInList() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found in your list");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private PrescriptionResponse toResponse(Prescription prescription) {
        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getPatient().getId(),
                prescription.getPatient().getUser().getFullName(),
                prescription.getDoctor().getUser().getFullName(),
                prescription.getMedicineName(),
                prescription.getDosage(),
                prescription.getFrequency(),
                List.copyOf(prescription.getAdministrationTimes()),
                prescription.getStartDate(),
                prescription.getEndDate(),
                prescription.getInstructions(),
                prescription.getNotes(),
                prescription.getStatus());
    }
}