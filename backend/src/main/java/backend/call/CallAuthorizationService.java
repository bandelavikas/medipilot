package backend.call;

import backend.entity.PatientProfile;
import backend.entity.Role;
import backend.entity.User;
import backend.repository.PatientProfileRepository;
import backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CallAuthorizationService {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;

    public CallAuthorizationService(UserRepository userRepository,
                                    PatientProfileRepository patientProfileRepository) {
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    // True only when one user is a doctor, the other is that doctor's own patient,
    // and both accounts are active.
    @Transactional(readOnly = true)
    public boolean isAuthorizedPair(Long firstUserId, Long secondUserId) {
        User first = userRepository.findById(firstUserId).orElse(null);
        User second = userRepository.findById(secondUserId).orElse(null);
        if (first == null || second == null || !first.isActive() || !second.isActive()) {
            return false;
        }

        User doctor;
        User patient;
        if (first.getRole() == Role.DOCTOR && second.getRole() == Role.PATIENT) {
            doctor = first;
            patient = second;
        } else if (first.getRole() == Role.PATIENT && second.getRole() == Role.DOCTOR) {
            doctor = second;
            patient = first;
        } else {
            return false;
        }

        return patientProfileRepository.findByUserId(patient.getId())
                .map(profile -> profile.getAssignedDoctor() != null
                        && profile.getAssignedDoctor().getUser().getId().equals(doctor.getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<DoctorContactResponse> findDoctorContact(Long patientUserId) {
        Optional<PatientProfile> profile = patientProfileRepository.findByUserId(patientUserId);
        if (profile.isEmpty() || profile.get().getAssignedDoctor() == null) {
            return Optional.empty();
        }

        var doctor = profile.get().getAssignedDoctor();
        return Optional.of(new DoctorContactResponse(
                doctor.getUser().getId(),
                doctor.getUser().getFullName(),
                doctor.getSpecialization()));
    }
}