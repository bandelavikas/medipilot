package backend.controller;

import backend.dto.DoseResponseRequest;
import backend.dto.ScheduledDoseResponse;
import backend.security.AuthenticatedUser;
import backend.service.DoseService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patient/schedule")
public class PatientScheduleController {

    private final DoseService doseService;

    public PatientScheduleController(DoseService doseService) {
        this.doseService = doseService;
    }

    @GetMapping
    public List<ScheduledDoseResponse> schedule(
            @AuthenticationPrincipal AuthenticatedUser patient,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = (from != null) ? from : LocalDate.now();
        LocalDate end = (to != null) ? to : start;
        return doseService.getSchedule(patient.id(), start, end);
    }

    @PostMapping("/{doseId}/respond")
    public ScheduledDoseResponse respond(
            @AuthenticationPrincipal AuthenticatedUser patient,
            @PathVariable Long doseId,
            @Valid @RequestBody DoseResponseRequest request) {
        return doseService.respond(patient.id(), doseId, request.status());
    }
}