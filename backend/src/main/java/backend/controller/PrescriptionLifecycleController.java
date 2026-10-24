package backend.controller;

import backend.dto.CancelPrescriptionResponse;
import backend.security.AuthenticatedUser;
import backend.service.PrescriptionLifecycleService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctor/prescriptions")
public class PrescriptionLifecycleController {

    private final PrescriptionLifecycleService lifecycleService;

    public PrescriptionLifecycleController(PrescriptionLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/{prescriptionId}/cancel")
    public CancelPrescriptionResponse cancel(@AuthenticationPrincipal AuthenticatedUser doctor,
                                             @PathVariable Long prescriptionId) {
        return lifecycleService.cancel(doctor.id(), prescriptionId);
    }
}