package backend.repository;

import backend.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {

    Optional<PatientProfile> findByUserId(Long userId);

    List<PatientProfile> findByAssignedDoctorId(Long doctorId);
}