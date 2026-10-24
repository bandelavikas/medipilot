package backend.controller;

import backend.dto.AdherenceReport;
import backend.security.AuthenticatedUser;
import backend.service.AdherenceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/patient/adherence")
public class PatientAdherenceController {

    private final AdherenceService adherenceService;

    public PatientAdherenceController(AdherenceService adherenceService) {
        this.adherenceService = adherenceService;
    }

    @GetMapping
    public AdherenceReport report(
            @AuthenticationPrincipal AuthenticatedUser patient,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(6);
        return adherenceService.reportForPatient(patient.id(), start, end);
    }
}