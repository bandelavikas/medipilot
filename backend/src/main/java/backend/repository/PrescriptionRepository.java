package backend.repository;

import backend.entity.Prescription;
import backend.entity.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByPatientId(Long patientId);

    List<Prescription> findByDoctorId(Long doctorId);

    long countByStatus(PrescriptionStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Prescription p set p.status = :completed "
            + "where p.status = :active and p.endDate < :today")
    int markCompleted(@Param("active") PrescriptionStatus active,
                      @Param("completed") PrescriptionStatus completed,
                      @Param("today") LocalDate today);
}