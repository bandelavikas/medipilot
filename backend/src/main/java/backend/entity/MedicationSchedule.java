package backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "medication_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_schedule_prescription_time",
                columnNames = {"prescription_id", "scheduled_at"}),
        indexes = @Index(name = "idx_schedule_scheduled_at", columnList = "scheduled_at"))
public class MedicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    // The exact date and time this dose is due.
    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    // Every dose starts as PENDING. Only the patient can change it to TAKEN or MISSED.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DoseStatus status = DoseStatus.PENDING;

    // When the patient confirmed Taken or Missed. Empty while the dose is still PENDING.
    private LocalDateTime respondedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public DoseStatus getStatus() {
        return status;
    }

    public void setStatus(DoseStatus status) {
        this.status = status;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}