package backend.repository;

import backend.entity.DoseStatus;
import backend.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {

    boolean existsByPrescriptionIdAndScheduledAt(Long prescriptionId, LocalDateTime scheduledAt);

    @Query("select s.scheduledAt from MedicationSchedule s where s.prescription.id = :prescriptionId")
    List<LocalDateTime> findScheduledTimes(@Param("prescriptionId") Long prescriptionId);

    @Query("select s from MedicationSchedule s "
            + "where s.prescription.patient.id = :patientId "
            + "and s.scheduledAt >= :from and s.scheduledAt < :to "
            + "order by s.scheduledAt")
    List<MedicationSchedule> findForPatientBetween(@Param("patientId") Long patientId,
                                                   @Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MedicationSchedule s "
            + "where s.prescription.id = :prescriptionId "
            + "and s.status = :status "
            + "and s.scheduledAt > :after")
    int deleteFuturePending(@Param("prescriptionId") Long prescriptionId,
                            @Param("status") DoseStatus status,
                            @Param("after") LocalDateTime after);
}