package backend.service;

import backend.dto.PatientSummary;
import backend.entity.DoctorProfile;
import backend.entity.PatientProfile;
import backend.entity.Role;
import backend.entity.User;
import backend.repository.DoctorProfileRepository;
import backend.repository.PatientProfileRepository;
import backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PatientService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final UserRepository userRepository;

    public PatientService(DoctorProfileRepository doctorProfileRepository,
                          PatientProfileRepository patientProfileRepository,
                          UserRepository userRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PatientSummary> listPatientsForDoctor(Long doctorUserId) {
        DoctorProfile doctor = findDoctor(doctorUserId);
        return patientProfileRepository.findByAssignedDoctorId(doctor.getId())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public PatientSummary assignPatient(Long doctorUserId, String email) {
        DoctorProfile doctor = findDoctor(doctorUserId);

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .filter(found -> found.getRole() == Role.PATIENT && found.isActive())
                .orElseThrow(this::patientNotFound);

        PatientProfile patient = patientProfileRepository.findByUserId(user.getId())
                .orElseThrow(this::patientNotFound);

        if (patient.getAssignedDoctor() != null) {
            if (patient.getAssignedDoctor().getId().equals(doctor.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This patient is already in your list");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This patient is already assigned to another doctor");
        }

        patient.setAssignedDoctor(doctor);
        patientProfileRepository.save(patient);
        return toSummary(patient);
    }

    private DoctorProfile findDoctor(Long doctorUserId) {
        return doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Doctor profile not found"));
    }

    private ResponseStatusException patientNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No patient account found with that email");
    }

    private PatientSummary toSummary(PatientProfile profile) {
        User user = profile.getUser();
        return new PatientSummary(
                profile.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                profile.getPhone(),
                profile.getDateOfBirth());
    }
}