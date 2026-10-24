package backend.controller;

import backend.dto.CreatePrescriptionRequest;
import backend.dto.PrescriptionResponse;
import backend.dto.ScheduleGenerationResponse;
import backend.security.AuthenticatedUser;
import backend.service.PrescriptionService;
import backend.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
public class DoctorPrescriptionController {

    private final PrescriptionService prescriptionService;
    private final ScheduleService scheduleService;

    public DoctorPrescriptionController(PrescriptionService prescriptionService,
                                        ScheduleService scheduleService) {
        this.prescriptionService = prescriptionService;
        this.scheduleService = scheduleService;
    }

    @PostMapping("/prescriptions")
    public ResponseEntity<PrescriptionResponse> create(
            @AuthenticationPrincipal AuthenticatedUser doctor,
            @Valid @RequestBody CreatePrescriptionRequest request) {
        PrescriptionResponse created = prescriptionService.create(doctor.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/patients/{patientId}/prescriptions")
    public List<PrescriptionResponse> listForPatient(
            @AuthenticationPrincipal AuthenticatedUser doctor,
            @PathVariable Long patientId) {
        return prescriptionService.listForDoctorPatient(doctor.id(), patientId);
    }

    @PostMapping("/prescriptions/{prescriptionId}/schedule")
    public ScheduleGenerationResponse generateSchedule(
            @AuthenticationPrincipal AuthenticatedUser doctor,
            @PathVariable Long prescriptionId) {
        int created = scheduleService.generateForDoctorPrescription(doctor.id(), prescriptionId);
        return new ScheduleGenerationResponse(created);
    }
}