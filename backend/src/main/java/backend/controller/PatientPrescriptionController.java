package backend.controller;

import backend.dto.PrescriptionResponse;
import backend.security.AuthenticatedUser;
import backend.service.PrescriptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patient/prescriptions")
public class PatientPrescriptionController {

    private final PrescriptionService prescriptionService;

    public PatientPrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping
    public List<PrescriptionResponse> myPrescriptions(@AuthenticationPrincipal AuthenticatedUser patient) {
        return prescriptionService.listForPatient(patient.id());
    }
}