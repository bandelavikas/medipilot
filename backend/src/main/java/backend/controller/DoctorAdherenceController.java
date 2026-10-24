package backend.controller;

import backend.dto.AdherenceReport;
import backend.security.AuthenticatedUser;
import backend.service.AdherenceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/doctor/patients")
public class DoctorAdherenceController {

    private final AdherenceService adherenceService;

    public DoctorAdherenceController(AdherenceService adherenceService) {
        this.adherenceService = adherenceService;
    }

    @GetMapping("/{patientId}/adherence")
    public AdherenceReport report(
            @AuthenticationPrincipal AuthenticatedUser doctor,
            @PathVariable Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(6);
        return adherenceService.reportForDoctor(doctor.id(), patientId, start, end);
    }
}